package com.keyloop.scheduler.availability;

import java.time.LocalDateTime;

public record Slot(LocalDateTime start, LocalDateTime end) {
}
