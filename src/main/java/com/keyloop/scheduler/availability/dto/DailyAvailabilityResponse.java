package com.keyloop.scheduler.availability.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyAvailabilityResponse(UUID dealershipId, UUID serviceTypeId, LocalDate date,
                                        int durationMinutes, List<SlotResponse> slots) {
}
