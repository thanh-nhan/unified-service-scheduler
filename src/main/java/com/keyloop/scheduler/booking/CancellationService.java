package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cancellation (design 04 §6): one conditional UPDATE, then a decision on the number of rows changed.
 * No optimistic locking and no retry; a concurrent cancel waits on the row lock and updates 0 rows.
 */
@Service
public class CancellationService {

    private final AppointmentRepository appointments;
    private final Clock clock;

    public CancellationService(AppointmentRepository appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Transactional
    public Appointment cancel(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = appointments.cancelIfFuture(id, now);

        Appointment appointment = appointments.findDetailedById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.APPOINTMENT_NOT_FOUND));
        if (updated == 1 || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return appointment;
        }
        throw new ApiException(ErrorCode.APPOINTMENT_ALREADY_STARTED);
    }
}
