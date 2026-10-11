package com.keyloop.scheduler.availability.dto;

import com.keyloop.scheduler.availability.AvailabilityService.DayAvailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record DayAvailabilityResponse(LocalDate date, DayOfWeek dayOfWeek, boolean open,
                                      List<SlotResponse> slots) {

    public static DayAvailabilityResponse from(DayAvailability day) {
        return new DayAvailabilityResponse(day.date(), day.date().getDayOfWeek(), day.open(),
                day.slots().stream().map(SlotResponse::from).toList());
    }
}
