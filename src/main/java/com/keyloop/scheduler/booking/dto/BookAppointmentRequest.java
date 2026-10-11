package com.keyloop.scheduler.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** {@code startTime} stays a String so it can be parsed strictly (design 04 §8). */
public record BookAppointmentRequest(
        @NotNull(message = "is required") UUID customerId,
        @NotNull(message = "is required") UUID vehicleId,
        @NotNull(message = "is required") UUID dealershipId,
        @NotNull(message = "is required") UUID serviceTypeId,
        @NotNull(message = "is required") String startTime) {
}
