package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.booking.dto.AppointmentResponse;
import com.keyloop.scheduler.booking.dto.BookAppointmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class BookingController {

    private final BookingService bookingService;
    private final AppointmentQueryService queryService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, AppointmentQueryService queryService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.queryService = queryService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BookAppointmentRequest request) {
        BookingResult result = bookingService.book(request, idempotencyKey);
        Appointment appointment = result.appointment();
        AppointmentResponse body = AppointmentMapper.toResponse(appointment);
        if (result.replayed()) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity
                .created(URI.create("/api/v1/appointments/" + appointment.getId()))
                .body(body);
    }

    @GetMapping("/{id}")
    public AppointmentResponse view(@PathVariable UUID id) {
        return AppointmentMapper.toResponse(queryService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id) {
        return AppointmentMapper.toResponse(cancellationService.cancel(id));
    }
}
