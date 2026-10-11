package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.availability.DailyHours;
import com.keyloop.scheduler.availability.SchedulingRules;
import com.keyloop.scheduler.booking.dto.BookAppointmentRequest;
import com.keyloop.scheduler.catalog.Dealership;
import com.keyloop.scheduler.catalog.DealershipRepository;
import com.keyloop.scheduler.catalog.OpeningHours;
import com.keyloop.scheduler.catalog.ServiceType;
import com.keyloop.scheduler.catalog.ServiceTypeRepository;
import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ApiException.FieldError;
import com.keyloop.scheduler.common.error.ErrorCode;
import com.keyloop.scheduler.common.persistence.ConstraintViolations;
import com.keyloop.scheduler.common.time.LocalDateTimes;
import com.keyloop.scheduler.customer.Customer;
import com.keyloop.scheduler.customer.CustomerRepository;
import com.keyloop.scheduler.customer.Vehicle;
import com.keyloop.scheduler.customer.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Booking flow (design 04 §2). Deliberately not {@code @Transactional}: every attempt runs in its own
 * transaction inside {@link BookingTransaction}, so a lost race can be retried with fresh data.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_KEY_LENGTH = 100;

    private final CustomerRepository customers;
    private final VehicleRepository vehicles;
    private final DealershipRepository dealerships;
    private final ServiceTypeRepository serviceTypes;
    private final AllocationQueries allocation;
    private final BookingTransaction bookingTransaction;
    private final IdempotencyService idempotency;
    private final BookingMetrics metrics;
    private final Clock clock;

    public BookingService(CustomerRepository customers, VehicleRepository vehicles,
                          DealershipRepository dealerships, ServiceTypeRepository serviceTypes,
                          AllocationQueries allocation, BookingTransaction bookingTransaction,
                          IdempotencyService idempotency, BookingMetrics metrics, Clock clock) {
        this.customers = customers;
        this.vehicles = vehicles;
        this.dealerships = dealerships;
        this.serviceTypes = serviceTypes;
        this.allocation = allocation;
        this.bookingTransaction = bookingTransaction;
        this.idempotency = idempotency;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** What the {@code booking.completed} event reports beyond the outcome. */
    private static final class Trace {
        int attempts;
        boolean replayed;
        String serviceTypeCode;
        String appointmentId;
    }

    /**
     * Runs the booking and emits exactly one {@code booking.completed} event and one outcome metric,
     * whether the booking succeeds, is rejected, or an unexpected exception escapes.
     */
    public BookingResult book(BookAppointmentRequest request, String idempotencyKey) {
        long startedAt = System.nanoTime();
        Trace trace = new Trace();
        String outcome = ErrorCode.INTERNAL_ERROR.name();
        try {
            BookingResult result = doBook(request, idempotencyKey, trace);
            outcome = BookingMetrics.CONFIRMED;
            trace.replayed = result.replayed();
            trace.appointmentId = String.valueOf(result.appointment().getId());
            return result;
        } catch (ApiException e) {
            outcome = e.code().name();
            throw e;
        } finally {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            metrics.recordOutcome(outcome, elapsed);
            var event = log.atInfo().setMessage("booking.completed")
                    .addKeyValue("outcome", outcome)
                    .addKeyValue("dealershipId", String.valueOf(request.dealershipId()))
                    .addKeyValue("attempts", trace.attempts)
                    .addKeyValue("durationMs", elapsed.toMillis())
                    .addKeyValue("replayed", trace.replayed);
            if (trace.serviceTypeCode != null) {
                event = event.addKeyValue("serviceTypeCode", trace.serviceTypeCode);
            }
            if (trace.appointmentId != null) {
                event = event.addKeyValue("appointmentId", trace.appointmentId);
            }
            event.log();
        }
    }

    private BookingResult doBook(BookAppointmentRequest request, String idempotencyKey, Trace trace) {
        // Step 1 (shape) is partly done by bean validation; the strict date-time parse is here.
        LocalDateTime start = LocalDateTimes.parse("startTime", request.startTime());

        // Step 2
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, null,
                    List.of(new FieldError("Idempotency-Key", "must be at most 100 characters")));
        }

        // Step 2a: replay of an earlier successful booking, before any existence check
        BookingCommand command = new BookingCommand(request.customerId(), request.vehicleId(),
                request.dealershipId(), request.serviceTypeId(), start, idempotencyKey,
                RequestHasher.hash(request.customerId(), request.vehicleId(), request.dealershipId(),
                        request.serviceTypeId(), start));
        Optional<Appointment> replay = idempotency.findReplay(command.customerId(), idempotencyKey,
                command.requestHash());
        if (replay.isPresent()) {
            return BookingResult.replay(replay.get());
        }

        // Step 3
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));
        Vehicle vehicle = vehicles.findById(request.vehicleId())
                .orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
        Dealership dealership = dealerships.findWithOpeningHoursById(request.dealershipId())
                .orElseThrow(() -> new ApiException(ErrorCode.DEALERSHIP_NOT_FOUND));
        ServiceType serviceType = serviceTypes.findById(request.serviceTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_TYPE_NOT_FOUND));
        trace.serviceTypeCode = serviceType.getCode();

        // Step 4
        if (!vehicle.belongsTo(customer.getId())) {
            throw new ApiException(ErrorCode.VEHICLE_NOT_OWNED_BY_CUSTOMER);
        }
        // Step 5
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new ApiException(ErrorCode.START_TIME_IN_PAST);
        }
        // Step 6
        if (!SchedulingRules.isOnGrid(start)) {
            throw new ApiException(ErrorCode.START_TIME_NOT_ALIGNED);
        }
        // Step 7
        LocalDateTime end = start.plusMinutes(serviceType.getDurationMinutes());
        if (!SchedulingRules.fitsOpeningHours(start, end, hoursFor(dealership, start))) {
            throw new ApiException(ErrorCode.OUTSIDE_OPENING_HOURS);
        }
        // Step 8
        if (allocation.vehicleHasOverlap(vehicle.getId(), start, end)) {
            // A concurrent duplicate may have committed since step 2a: its result wins over a conflict.
            return replayOrThrow(command, new ApiException(ErrorCode.VEHICLE_ALREADY_BOOKED));
        }

        // Step 9
        return attemptLoop(command, trace);
    }

    private BookingResult attemptLoop(BookingCommand command, Trace trace) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            trace.attempts = attempt;
            try {
                return BookingResult.created(bookingTransaction.tryAllocateAndInsert(command));
            } catch (NoCapacityException e) {
                return replayOrThrow(command, new ApiException(e.code()));
            } catch (DataIntegrityViolationException e) {
                // Always look the key up first: a duplicate request may have won (design 04 §4, rule 5).
                Optional<Appointment> replay = idempotency.findReplay(command.customerId(),
                        command.idempotencyKey(), command.requestHash());
                if (replay.isPresent()) {
                    return BookingResult.replay(replay.get());
                }
                String constraint = ConstraintViolations.constraintName(e).orElse("");
                switch (constraint) {
                    case "ex_appointment_vehicle_overlap" ->
                            throw new ApiException(ErrorCode.VEHICLE_ALREADY_BOOKED);
                    case "ex_appointment_technician_overlap", "ex_appointment_bay_overlap" ->
                            conflictRetry(constraint, attempt);
                    default -> throw e;
                }
            } catch (PessimisticLockingFailureException e) {
                // Two inserts that conflict on technician and bay in opposite order deadlock; PostgreSQL
                // aborts one. That is a lost race like any other, not a failure (not in design 04 §4).
                if (!ConstraintViolations.isDeadlock(e)) {
                    throw e;
                }
                Optional<Appointment> replay = idempotency.findReplay(command.customerId(),
                        command.idempotencyKey(), command.requestHash());
                if (replay.isPresent()) {
                    return BookingResult.replay(replay.get());
                }
                conflictRetry("deadlock", attempt);
            }
        }
        throw new ApiException(ErrorCode.HIGH_CONTENTION);
    }

    private void conflictRetry(String constraint, int attempt) {
        metrics.recordConflictRetry(constraint);
        log.atInfo().setMessage("booking.conflict.retry")
                .addKeyValue("constraint", constraint)
                .addKeyValue("attempt", attempt)
                .log();
    }

    private BookingResult replayOrThrow(BookingCommand command, ApiException conflict) {
        return idempotency.findReplay(command.customerId(), command.idempotencyKey(), command.requestHash())
                .map(BookingResult::replay)
                .orElseThrow(() -> conflict);
    }

    private static DailyHours hoursFor(Dealership dealership, LocalDateTime start) {
        for (OpeningHours h : dealership.getOpeningHours()) {
            if (h.getDayOfWeek() == start.getDayOfWeek()) {
                return new DailyHours(h.getOpensAt(), h.getClosesAt());
            }
        }
        return null;
    }
}
