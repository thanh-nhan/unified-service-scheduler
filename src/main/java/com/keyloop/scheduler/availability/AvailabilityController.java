package com.keyloop.scheduler.availability;

import com.keyloop.scheduler.availability.AvailabilityService.DayAvailability;
import com.keyloop.scheduler.availability.AvailabilityService.RangeAvailability;
import com.keyloop.scheduler.availability.dto.DailyAvailabilityResponse;
import com.keyloop.scheduler.availability.dto.SlotResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dealerships/{dealershipId}/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
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
}
