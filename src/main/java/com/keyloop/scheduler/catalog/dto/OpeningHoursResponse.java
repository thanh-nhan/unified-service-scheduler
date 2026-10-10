package com.keyloop.scheduler.catalog.dto;

import com.keyloop.scheduler.catalog.OpeningHours;

import java.time.format.DateTimeFormatter;

public record OpeningHoursResponse(String dayOfWeek, String opensAt, String closesAt) {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static OpeningHoursResponse from(OpeningHours hours) {
        return new OpeningHoursResponse(
                hours.getDayOfWeek().name(),
                hours.getOpensAt().format(HH_MM),
                hours.getClosesAt().format(HH_MM));
    }
}
