package com.keyloop.scheduler.availability;

import java.time.LocalDateTime;

public final class SchedulingRules {

    private SchedulingRules() {
    }

    /** GR-03: minutes in {0, 15, 30, 45}, seconds and nanos zero. */
    public static boolean isOnGrid(LocalDateTime t) {
        return t.getMinute() % 15 == 0 && t.getSecond() == 0 && t.getNano() == 0;
    }

    /** GR-04: the whole interval lies within the opening hours of its date; null hours means closed. */
    public static boolean fitsOpeningHours(LocalDateTime start, LocalDateTime end, DailyHours hours) {
        return hours != null
                && start.toLocalDate().equals(end.toLocalDate())
                && !start.toLocalTime().isBefore(hours.opensAt())
                && !end.toLocalTime().isAfter(hours.closesAt());
    }
}
