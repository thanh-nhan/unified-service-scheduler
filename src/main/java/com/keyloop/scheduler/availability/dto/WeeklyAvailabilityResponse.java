package com.keyloop.scheduler.availability.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WeeklyAvailabilityResponse(UUID dealershipId, UUID serviceTypeId, int durationMinutes,
                                         LocalDate weekStart, LocalDate weekEnd,
                                         List<DayAvailabilityResponse> days) {
}
