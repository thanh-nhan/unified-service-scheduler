package com.keyloop.scheduler.booking.dto;

import java.util.UUID;

/** Appointment representation (00-conventions). Deliberately has no customer email or phone (GR-12). */
public record AppointmentResponse(
        UUID id,
        String status,
        CustomerRef customer,
        VehicleRef vehicle,
        DealershipRef dealership,
        ServiceTypeRef serviceType,
        TechnicianRef technician,
        ServiceBayRef serviceBay,
        String startTime,
        String endTime,
        String createdAt,
        String cancelledAt) {

    public record CustomerRef(UUID id, String name) {
    }

    public record VehicleRef(UUID id, String vin, String make, String model) {
    }

    public record DealershipRef(UUID id, String name) {
    }

    public record ServiceTypeRef(UUID id, String code, String name, int durationMinutes) {
    }

    public record TechnicianRef(UUID id, String name) {
    }

    public record ServiceBayRef(UUID id, String name) {
    }
}
