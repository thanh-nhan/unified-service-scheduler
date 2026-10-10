package com.keyloop.scheduler.common;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import com.keyloop.scheduler.common.time.LocalDateTimes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDateTimesTest {

    @Test
    @DisplayName("GR-01: accepts a local date-time with seconds")
    void acceptsLocalDateTime() {
        assertThat(LocalDateTimes.parse("startTime", "2026-10-12T09:15:30"))
                .isEqualTo(LocalDateTime.of(2026, 10, 12, 9, 15, 30));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-12T09:30:00+07:00", "2026-10-12T02:30:00Z", "not-a-date", "", "2026-10-12"})
    @DisplayName("GR-01: rejects values with an offset, Z or bad format")
    void rejectsInvalid(String value) {
        assertThatThrownBy(() -> LocalDateTimes.parse("startTime", value))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
                    assertThat(e.errors()).singleElement()
                            .satisfies(err -> assertThat(err.field()).isEqualTo("startTime"));
                });
    }

    @Test
    @DisplayName("GR-01: rejects null")
    void rejectsNull() {
        assertThatThrownBy(() -> LocalDateTimes.parse("startTime", null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("GR-01: format always includes seconds")
    void formatIncludesSeconds() {
        assertThat(LocalDateTimes.format(LocalDateTime.of(2026, 10, 12, 9, 30)))
                .isEqualTo("2026-10-12T09:30:00");
    }
}
