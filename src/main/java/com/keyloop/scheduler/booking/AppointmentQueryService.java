package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppointmentQueryService {

    private final AppointmentRepository appointments;

    public AppointmentQueryService(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public Appointment getById(UUID id) {
        return appointments.findDetailedById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }
}
