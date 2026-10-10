package com.keyloop.scheduler.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class Fixture {

    public static final UUID D1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID D2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID S1 = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID S2 = UUID.fromString("40000000-0000-0000-0000-000000000002");
    public static final UUID S3 = UUID.fromString("40000000-0000-0000-0000-000000000003");
    public static final UUID T1 = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID T2 = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID T3 = UUID.fromString("20000000-0000-0000-0000-000000000003");
    public static final UUID T4 = UUID.fromString("20000000-0000-0000-0000-000000000004");
    public static final UUID T5 = UUID.fromString("20000000-0000-0000-0000-000000000005");
    public static final UUID B1 = UUID.fromString("30000000-0000-0000-0000-000000000001");
    public static final UUID B2 = UUID.fromString("30000000-0000-0000-0000-000000000002");
    public static final UUID B3 = UUID.fromString("30000000-0000-0000-0000-000000000003");
    public static final UUID B4 = UUID.fromString("30000000-0000-0000-0000-000000000004");
    public static final UUID C1 = UUID.fromString("50000000-0000-0000-0000-000000000001");
    public static final UUID C2 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    public static final UUID C3 = UUID.fromString("50000000-0000-0000-0000-000000000003");
    public static final UUID V1 = UUID.fromString("60000000-0000-0000-0000-000000000001");
    public static final UUID V2 = UUID.fromString("60000000-0000-0000-0000-000000000002");
    public static final UUID V3 = UUID.fromString("60000000-0000-0000-0000-000000000003");

    public static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    public static LocalDateTime at(String hhmm) {
        LocalTime time = LocalTime.parse(hhmm);
        return LocalDateTime.of(MONDAY, time);
    }
}
