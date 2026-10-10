package com.keyloop.scheduler.catalog.dto;

import com.keyloop.scheduler.catalog.Dealership;
import com.keyloop.scheduler.catalog.OpeningHours;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record DealershipResponse(UUID id, String name, List<OpeningHoursResponse> openingHours) {

    public static DealershipResponse from(Dealership dealership) {
        List<OpeningHoursResponse> hours = dealership.getOpeningHours().stream()
                .sorted(Comparator.comparing(OpeningHours::getDayOfWeek))
                .map(OpeningHoursResponse::from)
                .toList();
        return new DealershipResponse(dealership.getId(), dealership.getName(), hours);
    }
}
