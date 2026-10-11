package com.keyloop.scheduler.booking;

import java.time.LocalDateTime;
import java.util.UUID;

/** A validated booking request, ready to be allocated and stored. */
public record BookingCommand(
        UUID customerId,
        UUID vehicleId,
        UUID dealershipId,
        UUID serviceTypeId,
        LocalDateTime startTime,
        String idempotencyKey,
        String requestHash) {
}
