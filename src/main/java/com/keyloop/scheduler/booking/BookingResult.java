package com.keyloop.scheduler.booking;

/** Outcome of a booking request: the appointment, and whether it is a replay of an earlier success. */
public record BookingResult(Appointment appointment, boolean replayed) {

    public static BookingResult created(Appointment appointment) {
        return new BookingResult(appointment, false);
    }

    public static BookingResult replay(Appointment appointment) {
        return new BookingResult(appointment, true);
    }
}
