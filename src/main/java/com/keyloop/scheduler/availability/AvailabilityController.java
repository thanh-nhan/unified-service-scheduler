package com.keyloop.scheduler.availability;

import com.keyloop.scheduler.availability.AvailabilityService.DayAvailability;
import com.keyloop.scheduler.availability.AvailabilityService.RangeAvailability;
import com.keyloop.scheduler.availability.dto.DailyAvailabilityResponse;
import com.keyloop.scheduler.availability.dto.DayAvailabilityResponse;
import com.keyloop.scheduler.availability.dto.SlotResponse;
import com.keyloop.scheduler.availability.dto.WeeklyAvailabilityResponse;
import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ApiException.FieldError;
import com.keyloop.scheduler.common.error.ErrorCode;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dealerships/{dealershipId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final Clock clock;

    public AvailabilityController(AvailabilityService availabilityService, Clock clock) {
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    @GetMapping
    public DailyAvailabilityResponse daily(
            @PathVariable UUID dealershipId,
            @RequestParam UUID serviceTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RangeAvailability range = availabilityService.availability(
                dealershipId, serviceTypeId, date, date.plusDays(1));
        DayAvailability day = range.days().get(0);
        return new DailyAvailabilityResponse(dealershipId, serviceTypeId, date, range.durationMinutes(),
                day.slots().stream().map(SlotResponse::from).toList());
    }

    @GetMapping("/weekly")
    public WeeklyAvailabilityResponse weekly(
            @PathVariable UUID dealershipId,
            @RequestParam UUID serviceTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        if (weekStart == null) {
            weekStart = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, null,
                    List.of(new FieldError("weekStart", "must be a Monday")));
        }
        RangeAvailability range = availabilityService.availability(
                dealershipId, serviceTypeId, weekStart, weekStart.plusDays(7));
        return new WeeklyAvailabilityResponse(dealershipId, serviceTypeId, range.durationMinutes(),
                weekStart, weekStart.plusDays(6),
                range.days().stream().map(DayAvailabilityResponse::from).toList());
    }
}
