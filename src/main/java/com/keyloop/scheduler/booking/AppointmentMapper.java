package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.booking.dto.AppointmentResponse;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.CustomerRef;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.DealershipRef;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.ServiceBayRef;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.ServiceTypeRef;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.TechnicianRef;
import com.keyloop.scheduler.booking.dto.AppointmentResponse.VehicleRef;
import com.keyloop.scheduler.common.time.LocalDateTimes;

import java.time.LocalDateTime;

public final class AppointmentMapper {

    private AppointmentMapper() {
    }

    public static AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getStatus().name(),
                new CustomerRef(a.getCustomer().getId(), a.getCustomer().getName()),
                new VehicleRef(a.getVehicle().getId(), a.getVehicle().getVin(),
                        a.getVehicle().getMake(), a.getVehicle().getModel()),
                new DealershipRef(a.getDealership().getId(), a.getDealership().getName()),
                new ServiceTypeRef(a.getServiceType().getId(), a.getServiceType().getCode(),
                        a.getServiceType().getName(), a.getServiceType().getDurationMinutes()),
                new TechnicianRef(a.getTechnician().getId(), a.getTechnician().getName()),
                new ServiceBayRef(a.getServiceBay().getId(), a.getServiceBay().getName()),
                format(a.getStartTime()),
                format(a.getEndTime()),
                format(a.getCreatedAt()),
                format(a.getCancelledAt()));
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : LocalDateTimes.format(value);
    }
}
