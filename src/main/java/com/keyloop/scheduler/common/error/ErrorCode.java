package com.keyloop.scheduler.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request is invalid. Please check the highlighted fields and try again."),
    IDEMPOTENCY_KEY_MISSING(HttpStatus.BAD_REQUEST, "The Idempotency-Key header is required."),

    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "The customer could not be found."),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "The vehicle could not be found."),
    DEALERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "The dealership could not be found."),
    SERVICE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "The service type could not be found."),
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "The appointment could not be found."),

    NO_QUALIFIED_TECHNICIAN_AVAILABLE(HttpStatus.CONFLICT,
            "No technician is available for the selected time. Please choose another time."),
    NO_SERVICE_BAY_AVAILABLE(HttpStatus.CONFLICT,
            "No service bay is available for the selected time. Please choose another time."),
    HIGH_CONTENTION(HttpStatus.CONFLICT,
            "We are very busy right now and could not complete your booking. Please try again."),
    VEHICLE_ALREADY_BOOKED(HttpStatus.CONFLICT,
            "This vehicle already has an appointment at the selected time."),

    VEHICLE_NOT_OWNED_BY_CUSTOMER(HttpStatus.UNPROCESSABLE_CONTENT,
            "The vehicle does not belong to this customer."),
    START_TIME_IN_PAST(HttpStatus.UNPROCESSABLE_CONTENT, "The start time must be in the future."),
    START_TIME_NOT_ALIGNED(HttpStatus.UNPROCESSABLE_CONTENT,
            "The start time must be on a 15-minute boundary (00, 15, 30 or 45)."),
    OUTSIDE_OPENING_HOURS(HttpStatus.UNPROCESSABLE_CONTENT,
            "The appointment must be within the opening hours of the dealership."),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT,
            "This Idempotency-Key was already used with a different request."),
    APPOINTMENT_ALREADY_STARTED(HttpStatus.UNPROCESSABLE_CONTENT,
            "The appointment has already started and can no longer be changed."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong on our side. Please try again later.");

    private final HttpStatus status;
    private final String defaultDetail;

    ErrorCode(HttpStatus status, String defaultDetail) {
        this.status = status;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultDetail() {
        return defaultDetail;
    }
}
