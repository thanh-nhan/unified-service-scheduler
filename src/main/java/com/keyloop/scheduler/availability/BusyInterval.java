package com.keyloop.scheduler.availability;

import java.time.LocalDateTime;

public record BusyInterval(LocalDateTime start, LocalDateTime end) {

    /** Half-open overlap with [s, e): back-to-back intervals do not overlap (GR-02). */
    public boolean overlaps(LocalDateTime s, LocalDateTime e) {
        return start.isBefore(e) && end.isAfter(s);
    }
}
