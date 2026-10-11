package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.booking.dto.AppointmentResponse;
import com.keyloop.scheduler.booking.dto.BookAppointmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/appointments")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
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
}
