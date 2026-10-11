package com.keyloop.scheduler.booking;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Business metrics for booking (design 08 §3). Tags are low-cardinality: {@code outcome} is
 * {@code CONFIRMED} or an error code, {@code constraint} is one of {@link #CONSTRAINTS}.
 */
@Component
public class BookingMetrics {

    public static final String CONFIRMED = "CONFIRMED";

    /** Constraint tag values for lost races; {@code deadlock} is a race lost to a PostgreSQL deadlock abort. */
    static final List<String> CONSTRAINTS = List.of(
            "ex_appointment_technician_overlap", "ex_appointment_bay_overlap", "deadlock");

    private final MeterRegistry registry;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;
        // Registered up front so the series exist (at zero) before the first conflict.
        CONSTRAINTS.forEach(c -> registry.counter("scheduler.booking.conflict.retries", "constraint", c));
        registry.counter("scheduler.booking.attempts", "outcome", CONFIRMED);
    }

    public void recordOutcome(String outcome, Duration duration) {
        registry.counter("scheduler.booking.attempts", "outcome", outcome).increment();
        Timer.builder("scheduler.booking.duration")
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }

    public void recordConflictRetry(String constraint) {
        registry.counter("scheduler.booking.conflict.retries", "constraint", constraint).increment();
    }
}
