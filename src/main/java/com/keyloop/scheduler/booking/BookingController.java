package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.booking.dto.AppointmentResponse;
import com.keyloop.scheduler.booking.dto.BookAppointmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Appointments", description = "Book, view and cancel service appointments")
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

    @Operation(summary = "Book an appointment",
            description = "Idempotent per customer and Idempotency-Key: a replay returns 200 with the original appointment.")
    @ApiResponse(responseCode = "201", description = "Appointment created")
    @ApiResponse(responseCode = "200", description = "Replay of an earlier request with the same Idempotency-Key")
    @ApiResponse(responseCode = "400", description = "Malformed request or missing Idempotency-Key",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer, vehicle, dealership or service type not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "No technician or bay available, vehicle already booked, or high contention",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Business rule violated (past or unaligned start, outside opening hours, vehicle not owned, key reused)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(
            @Parameter(required = true, description = "Client-generated key, at most 100 characters")
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

    @Operation(summary = "Get an appointment")
    @ApiResponse(responseCode = "404", description = "Appointment not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public AppointmentResponse view(@PathVariable UUID id) {
        return AppointmentMapper.toResponse(queryService.getById(id));
    }

    @Operation(summary = "Cancel an appointment", description = "Idempotent: cancelling a cancelled appointment returns it unchanged.")
    @ApiResponse(responseCode = "404", description = "Appointment not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Appointment has already started",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id) {
        return AppointmentMapper.toResponse(cancellationService.cancel(id));
    }
}
