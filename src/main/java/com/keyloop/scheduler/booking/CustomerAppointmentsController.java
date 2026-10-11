package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.booking.dto.AppointmentPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/appointments")
public class CustomerAppointmentsController {

    private final AppointmentQueryService queryService;

    public CustomerAppointmentsController(AppointmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public AppointmentPageResponse list(
            @PathVariable UUID customerId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<Appointment> result = queryService.listForCustomer(customerId, status, page, size);
        return AppointmentPageResponse.of(result, AppointmentMapper::toResponse);
    }
}
