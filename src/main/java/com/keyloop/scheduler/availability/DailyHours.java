package com.keyloop.scheduler.availability;

import java.time.LocalTime;

public record DailyHours(LocalTime opensAt, LocalTime closesAt) {
}
