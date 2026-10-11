package com.keyloop.scheduler.availability;

import com.keyloop.scheduler.availability.AvailabilityQueries.BusyRow;
import com.keyloop.scheduler.catalog.Dealership;
import com.keyloop.scheduler.catalog.DealershipRepository;
import com.keyloop.scheduler.catalog.OpeningHours;
import com.keyloop.scheduler.catalog.ServiceType;
import com.keyloop.scheduler.catalog.ServiceTypeRepository;
import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    /** One calendar day of availability; {@code open} is false when the dealership has no hours that day. */
    public record DayAvailability(LocalDate date, boolean open, List<Slot> slots) {
    }

    public record RangeAvailability(int durationMinutes, List<DayAvailability> days) {
    }

    private final DealershipRepository dealerships;
    private final ServiceTypeRepository serviceTypes;
    private final AvailabilityQueries queries;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public AvailabilityService(DealershipRepository dealerships, ServiceTypeRepository serviceTypes,
                               AvailabilityQueries queries, MeterRegistry meterRegistry, Clock clock) {
        this.dealerships = dealerships;
        this.serviceTypes = serviceTypes;
        this.queries = queries;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    /** Availability for each date in [fromDate, toDateExclusive), using a fixed number of queries. */
    public RangeAvailability availability(UUID dealershipId, UUID serviceTypeId,
                                          LocalDate fromDate, LocalDate toDateExclusive) {
        long startedAt = System.nanoTime();
        try {
            return calculate(dealershipId, serviceTypeId, fromDate, toDateExclusive);
        } finally {
            String range = fromDate.plusDays(1).equals(toDateExclusive) ? "day" : "week";
            Timer.builder("scheduler.availability.duration")
                    .tag("range", range)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    private RangeAvailability calculate(UUID dealershipId, UUID serviceTypeId,
                                        LocalDate fromDate, LocalDate toDateExclusive) {
        Dealership dealership = dealerships.findWithOpeningHoursById(dealershipId)
                .orElseThrow(() -> new ApiException(ErrorCode.DEALERSHIP_NOT_FOUND));
        ServiceType serviceType = serviceTypes.findById(serviceTypeId)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_TYPE_NOT_FOUND));

        Set<UUID> technicianIds = queries.qualifiedTechnicianIds(dealershipId, serviceType.getRequiredSkill().name());
        Set<UUID> bayIds = queries.eligibleBayIds(dealershipId, serviceType.getRequiredBayType().name());
        List<BusyRow> rows = queries.busyIntervals(technicianIds, bayIds,
                fromDate.atStartOfDay(), toDateExclusive.atStartOfDay());

        Map<UUID, List<BusyInterval>> busy = new HashMap<>();
        for (BusyRow row : rows) {
            BusyInterval interval = new BusyInterval(row.start(), row.end());
            busy.computeIfAbsent(row.technicianId(), k -> new ArrayList<>()).add(interval);
            busy.computeIfAbsent(row.bayId(), k -> new ArrayList<>()).add(interval);
        }

        Map<DayOfWeek, DailyHours> hoursByDay = new HashMap<>();
        for (OpeningHours h : dealership.getOpeningHours()) {
            hoursByDay.put(h.getDayOfWeek(), new DailyHours(h.getOpensAt(), h.getClosesAt()));
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<DayAvailability> days = new ArrayList<>();
        for (LocalDate date = fromDate; date.isBefore(toDateExclusive); date = date.plusDays(1)) {
            DailyHours hours = hoursByDay.get(date.getDayOfWeek());
            List<Slot> slots = AvailabilityCalculator.calculate(date, hours,
                    serviceType.getDurationMinutes(), technicianIds, bayIds, busy, now);
            days.add(new DayAvailability(date, hours != null, slots));
        }
        return new RangeAvailability(serviceType.getDurationMinutes(), days);
    }
}
