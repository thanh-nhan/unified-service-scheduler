package com.keyloop.scheduler.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class MutableClock extends Clock {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalDateTime DEFAULT_NOW = LocalDateTime.of(2026, 10, 11, 10, 0);

    private volatile Instant instant = toInstant(DEFAULT_NOW);

    public void setNow(LocalDateTime now) {
        this.instant = toInstant(now);
    }

    public void reset() {
        this.instant = toInstant(DEFAULT_NOW);
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZONE).toInstant();
    }
}
