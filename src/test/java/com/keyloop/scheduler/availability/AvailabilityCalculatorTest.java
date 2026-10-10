package com.keyloop.scheduler.availability;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityCalculatorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 12);
    private static final DailyHours HOURS = new DailyHours(LocalTime.of(8, 0), LocalTime.of(17, 0));
    private static final LocalDateTime NOW = DATE.minusDays(1).atTime(10, 0);

    private static final UUID T1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID T2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID B1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID B2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static LocalDateTime at(int h, int m) {
        return DATE.atTime(h, m);
    }

    private static BusyInterval busy(int h1, int m1, int h2, int m2) {
        return new BusyInterval(at(h1, m1), at(h2, m2));
    }

    private static List<Slot> calc(int duration, Set<UUID> techs, Set<UUID> bays,
                                   Map<UUID, List<BusyInterval>> busy, LocalDateTime now) {
        return AvailabilityCalculator.calculate(DATE, HOURS, duration, techs, bays, busy, now);
    }

    private static List<Slot> calc(int duration, Map<UUID, List<BusyInterval>> busy) {
        return calc(duration, Set.of(T1, T2), Set.of(B1, B2), busy, NOW);
    }

    private static List<LocalDateTime> starts(List<Slot> slots) {
        return slots.stream().map(Slot::start).toList();
    }

    @Test
    void overlapsHalfOpen() {
        var existing = busy(9, 0, 10, 0);
        assertThat(existing.overlaps(at(9, 30), at(10, 0))).isTrue();
        assertThat(existing.overlaps(at(10, 0), at(11, 0))).isFalse();
        assertThat(existing.overlaps(at(8, 0), at(9, 0))).isFalse();
        assertThat(existing.overlaps(at(8, 30), at(11, 0))).isTrue();
        assertThat(existing.overlaps(at(9, 15), at(9, 45))).isTrue();
    }

    @Test
    void noBusyIntervals() {
        var slots = calc(30, Map.of());
        assertThat(slots).hasSize(35);
        assertThat(slots.get(0).start()).isEqualTo(at(8, 0));
        assertThat(slots.get(34).start()).isEqualTo(at(16, 30));
        assertThat(slots.get(34).end()).isEqualTo(at(17, 0));
        assertThat(starts(slots)).isSorted();
    }

    @Test
    void longServiceWithOnlyTechnicianBusy() {
        var slots = calc(90, Set.of(T1), Set.of(B1, B2), Map.of(T1, List.of(busy(9, 0, 10, 30))), NOW);
        assertThat(slots).hasSize(21);
        assertThat(slots.get(0).start()).isEqualTo(at(10, 30));
    }

    @Test
    void bothTechniciansBusy() {
        var b = Map.of(T1, List.of(busy(9, 0, 9, 30)), T2, List.of(busy(9, 0, 9, 30)));
        assertThat(starts(calc(30, b)))
                .doesNotContain(at(8, 45), at(9, 0), at(9, 15))
                .contains(at(8, 30), at(9, 30));
    }

    @Test
    void oneTechnicianBusyOtherFree() {
        var b = Map.of(T1, List.of(busy(9, 0, 9, 30)));
        assertThat(starts(calc(30, b))).contains(at(9, 0));
    }

    @Test
    void allBaysBusy() {
        var b = Map.of(B1, List.of(busy(9, 0, 9, 30)), B2, List.of(busy(9, 0, 9, 30)));
        assertThat(starts(calc(30, b))).doesNotContain(at(9, 0)).contains(at(9, 30));
    }

    @Test
    void pastTimesExcluded() {
        var slots = calc(30, Set.of(T1, T2), Set.of(B1, B2), Map.of(), at(10, 5));
        assertThat(slots.get(0).start()).isEqualTo(at(10, 15));
    }

    @Test
    void slotAtExactlyNowExcluded() {
        var slots = calc(30, Set.of(T1), Set.of(B1), Map.of(), at(10, 0));
        assertThat(slots.get(0).start()).isEqualTo(at(10, 15));
    }

    @Test
    void closedDayAndEmptyResources() {
        assertThat(AvailabilityCalculator.calculate(DATE, null, 30, Set.of(T1), Set.of(B1), Map.of(), NOW)).isEmpty();
        assertThat(calc(30, Set.of(), Set.of(B1, B2), Map.of(), NOW)).isEmpty();
        assertThat(calc(30, Set.of(T1, T2), Set.of(), Map.of(), NOW)).isEmpty();
    }

    @Test
    void durationLongerThanDay() {
        assertThat(calc(600, Map.of())).isEmpty();
    }
}
