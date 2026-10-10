package com.keyloop.scheduler.availability;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingRulesTest {

    private static final DailyHours HOURS = new DailyHours(LocalTime.of(8, 0), LocalTime.of(17, 0));

    private static LocalDateTime at(int h, int m) {
        return LocalDateTime.of(2026, 10, 12, h, m);
    }

    @Test
    void onGrid() {
        assertTrue(SchedulingRules.isOnGrid(at(9, 0)));
        assertTrue(SchedulingRules.isOnGrid(at(9, 15)));
        assertTrue(SchedulingRules.isOnGrid(at(9, 45)));
    }

    @Test
    void offGrid() {
        assertFalse(SchedulingRules.isOnGrid(at(9, 10)));
        assertFalse(SchedulingRules.isOnGrid(at(9, 15).withSecond(30)));
        assertFalse(SchedulingRules.isOnGrid(at(9, 15).withNano(1)));
    }

    @Test
    void fitsOpeningHours() {
        assertTrue(SchedulingRules.fitsOpeningHours(at(8, 0), at(8, 30), HOURS));
        assertTrue(SchedulingRules.fitsOpeningHours(at(16, 30), at(17, 0), HOURS));
    }

    @Test
    void doesNotFitOpeningHours() {
        assertFalse(SchedulingRules.fitsOpeningHours(at(7, 45), at(8, 15), HOURS));
        assertFalse(SchedulingRules.fitsOpeningHours(at(16, 45), at(17, 15), HOURS));
        assertFalse(SchedulingRules.fitsOpeningHours(at(8, 0), at(8, 30), null));
        assertFalse(SchedulingRules.fitsOpeningHours(at(16, 30), at(17, 0).plusDays(1), HOURS));
    }
}
