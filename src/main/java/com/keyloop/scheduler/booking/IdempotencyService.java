package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Idempotent replay lookup (design 04 §5). Only successful bookings are remembered, because the key
 * lives on the appointment row. Deliberately not transactional: it must be callable right after a
 * failed attempt transaction has been rolled back.
 */
@Service
public class IdempotencyService {

    private final AppointmentRepository appointments;

    public IdempotencyService(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    /**
     * @return the original appointment when this customer already booked successfully with the key and the
     *         same request; empty when the key is unused
     * @throws ApiException {@code IDEMPOTENCY_KEY_REUSED} when the key was used with a different request
     */
    public Optional<Appointment> findReplay(UUID customerId, String idempotencyKey, String requestHash) {
        return appointments.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(existing -> {
                    if (!existing.getRequestHash().equals(requestHash)) {
                        throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
                    }
                    // Reload with associations so the response can be built outside a transaction.
                    return appointments.findDetailedById(existing.getId()).orElseThrow();
                });
    }
}
