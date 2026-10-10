package com.keyloop.scheduler.common.time;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ApiException.FieldError;
import com.keyloop.scheduler.common.error.ErrorCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class LocalDateTimes {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String INVALID_MESSAGE =
            "must be a local date-time without offset, e.g. 2026-10-12T09:30:00";

    private LocalDateTimes() {
    }

    public static LocalDateTime parse(String field, String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, null,
                    List.of(new FieldError(field, INVALID_MESSAGE)));
        }
    }

    public static String format(LocalDateTime value) {
        return FORMATTER.format(value);
    }
}
