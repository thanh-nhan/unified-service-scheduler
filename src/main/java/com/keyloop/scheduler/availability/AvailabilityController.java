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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Availability", description = "Bookable start times for a service type")
@RequestMapping("/api/v1/dealerships/{dealershipId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final Clock clock;

    public AvailabilityController(AvailabilityService availabilityService, Clock clock) {
        this.availabilityService = availabilityService;
        this.clock = clock;
    }

    @Operation(summary = "Available slots for one day")
    @ApiResponse(responseCode = "400", description = "Malformed date or service type id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Dealership or service type not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
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

    @Operation(summary = "Available slots for a week",
            description = "weekStart must be a Monday; defaults to the current week.")
    @ApiResponse(responseCode = "400", description = "Malformed parameters, or weekStart is not a Monday",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Dealership or service type not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
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
