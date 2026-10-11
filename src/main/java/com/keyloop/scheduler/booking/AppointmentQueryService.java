package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import com.keyloop.scheduler.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppointmentQueryService {

    private final AppointmentRepository appointments;
    private final CustomerRepository customers;

    public AppointmentQueryService(AppointmentRepository appointments, CustomerRepository customers) {
        this.appointments = appointments;
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public Appointment getById(UUID id) {
        return appointments.findDetailedById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Appointment> listForCustomer(UUID customerId, AppointmentStatus status, int page, int size) {
        if (!customers.existsById(customerId)) {
            throw new ApiException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return appointments.findPageForCustomer(customerId, status, PageRequest.of(page, size));
    }
}
