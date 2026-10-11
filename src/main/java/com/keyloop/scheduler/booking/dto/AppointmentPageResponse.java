package com.keyloop.scheduler.booking.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record AppointmentPageResponse(
        List<AppointmentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public static <T> AppointmentPageResponse of(Page<T> page, Function<T, AppointmentResponse> mapper) {
        return new AppointmentPageResponse(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
