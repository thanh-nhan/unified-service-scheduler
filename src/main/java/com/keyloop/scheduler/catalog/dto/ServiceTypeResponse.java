package com.keyloop.scheduler.catalog.dto;

import com.keyloop.scheduler.catalog.ServiceType;

import java.util.UUID;

public record ServiceTypeResponse(UUID id, String code, String name, int durationMinutes) {

    public static ServiceTypeResponse from(ServiceType serviceType) {
        return new ServiceTypeResponse(
                serviceType.getId(),
                serviceType.getCode(),
                serviceType.getName(),
                serviceType.getDurationMinutes());
    }
}
