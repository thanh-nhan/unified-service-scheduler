package com.keyloop.scheduler.availability;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AvailabilityCalculator {

    private static final int STEP_MINUTES = 15;

    private AvailabilityCalculator() {
    }

    /**
     * Candidate starts every 15 minutes from opening while the service still ends by closing time.
     * A slot is available if it is strictly after {@code now} and at least one technician and
     * one bay are free for the whole interval. Result is in ascending order.
     */
    public static List<Slot> calculate(LocalDate date,
                                       DailyHours hours,
                                       int durationMinutes,
                                       Set<UUID> technicianIds,
                                       Set<UUID> bayIds,
                                       Map<UUID, List<BusyInterval>> busy,
                                       LocalDateTime now) {
        if (hours == null) {
            return List.of();
        }
        List<Slot> slots = new ArrayList<>();
        LocalDateTime close = date.atTime(hours.closesAt());
        for (LocalDateTime s = date.atTime(hours.opensAt());
             !s.plusMinutes(durationMinutes).isAfter(close);
             s = s.plusMinutes(STEP_MINUTES)) {
            if (!s.isAfter(now)) {
                continue;
            }
            LocalDateTime e = s.plusMinutes(durationMinutes);
            if (anyFree(technicianIds, busy, s, e) && anyFree(bayIds, busy, s, e)) {
                slots.add(new Slot(s, e));
            }
        }
        return slots;
    }

    private static boolean anyFree(Set<UUID> ids, Map<UUID, List<BusyInterval>> busy,
                                   LocalDateTime s, LocalDateTime e) {
        return ids.stream().anyMatch(id ->
                busy.getOrDefault(id, List.of()).stream().noneMatch(b -> b.overlaps(s, e)));
    }
}
