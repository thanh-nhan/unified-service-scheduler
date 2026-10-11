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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

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
    private final Clock clock;

    public BookingService(CustomerRepository customers, VehicleRepository vehicles,
                          DealershipRepository dealerships, ServiceTypeRepository serviceTypes,
                          AllocationQueries allocation, BookingTransaction bookingTransaction, Clock clock) {
        this.customers = customers;
        this.vehicles = vehicles;
        this.dealerships = dealerships;
        this.serviceTypes = serviceTypes;
        this.allocation = allocation;
        this.bookingTransaction = bookingTransaction;
        this.clock = clock;
    }

    public Appointment book(BookAppointmentRequest request, String idempotencyKey) {
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

        // Step 3
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_NOT_FOUND));
        Vehicle vehicle = vehicles.findById(request.vehicleId())
                .orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
        Dealership dealership = dealerships.findWithOpeningHoursById(request.dealershipId())
                .orElseThrow(() -> new ApiException(ErrorCode.DEALERSHIP_NOT_FOUND));
        ServiceType serviceType = serviceTypes.findById(request.serviceTypeId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_TYPE_NOT_FOUND));

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
            throw new ApiException(ErrorCode.VEHICLE_ALREADY_BOOKED);
        }

        // Step 9
        BookingCommand command = new BookingCommand(customer.getId(), vehicle.getId(), dealership.getId(),
                serviceType.getId(), start, idempotencyKey,
                RequestHasher.hash(customer.getId(), vehicle.getId(), dealership.getId(), serviceType.getId(),
                        start));
        return attemptLoop(command);
    }

    private Appointment attemptLoop(BookingCommand command) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return bookingTransaction.tryAllocateAndInsert(command);
            } catch (NoCapacityException e) {
                throw new ApiException(e.code());
            } catch (DataIntegrityViolationException e) {
                String constraint = ConstraintViolations.constraintName(e).orElse("");
                switch (constraint) {
                    case "ex_appointment_vehicle_overlap" ->
                            throw new ApiException(ErrorCode.VEHICLE_ALREADY_BOOKED);
                    case "ex_appointment_technician_overlap", "ex_appointment_bay_overlap" ->
                            log.info("Booking attempt {} lost a race on {}", attempt, constraint);
                    default -> throw e;
                }
            }
        }
        throw new ApiException(ErrorCode.HIGH_CONTENTION);
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
