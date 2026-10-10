package com.keyloop.scheduler.availability.dto;

import com.keyloop.scheduler.availability.Slot;
import com.keyloop.scheduler.common.time.LocalDateTimes;

public record SlotResponse(String startTime, String endTime) {

    public static SlotResponse from(Slot slot) {
        return new SlotResponse(LocalDateTimes.format(slot.start()), LocalDateTimes.format(slot.end()));
    }
}
