// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.jayway.jsonpath.JsonPath;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B2;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.D2;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.S3;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeeklyAvailabilityAcceptanceTest extends IntegrationTest {

    private static final String WEEK_START = "2026-10-12";
    private static final String[] DAY_NAMES =
            {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};

    private ResultActions weekly(UUID dealershipId, UUID serviceTypeId, String weekStart) throws Exception {
        var request = get("/api/v1/dealerships/{id}/availability/weekly", dealershipId);
        if (serviceTypeId != null) {
            request.param("serviceTypeId", serviceTypeId.toString());
        }
        if (weekStart != null) {
            request.param("weekStart", weekStart);
        }
        return mockMvc.perform(request);
    }

    private ResultActions daily(UUID dealershipId, UUID serviceTypeId, String date) throws Exception {
        return mockMvc.perform(get("/api/v1/dealerships/{id}/availability", dealershipId)
                .param("serviceTypeId", serviceTypeId.toString())
                .param("date", date));
    }

    private static LocalDateTime tuesday(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 13, hour, minute);
    }

    @Test
    @DisplayName("AC-09.1: Full week with no appointments")
    void fullWeekNoAppointments() throws Exception {
        ResultActions result = weekly(D1, S1, WEEK_START)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealershipId").value(D1.toString()))
                .andExpect(jsonPath("$.serviceTypeId").value(S1.toString()))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.weekStart").value("2026-10-12"))
                .andExpect(jsonPath("$.weekEnd").value("2026-10-18"))
                .andExpect(jsonPath("$.days.length()").value(7));

        for (int i = 0; i < 7; i++) {
            String date = LocalDate.parse(WEEK_START).plusDays(i).toString();
            String day = "$.days[" + i + "]";
            result.andExpect(jsonPath(day + ".date").value(date))
                    .andExpect(jsonPath(day + ".dayOfWeek").value(DAY_NAMES[i]));
            if (i < 6) {
                result.andExpect(jsonPath(day + ".open").value(true))
                        .andExpect(jsonPath(day + ".slots.length()").value(35))
                        .andExpect(jsonPath(day + ".slots[0].startTime").value(date + "T08:00:00"))
                        .andExpect(jsonPath(day + ".slots[0].endTime").value(date + "T08:30:00"))
                        .andExpect(jsonPath(day + ".slots[34].startTime").value(date + "T16:30:00"))
                        .andExpect(jsonPath(day + ".slots[34].endTime").value(date + "T17:00:00"));
            } else {
                result.andExpect(jsonPath(day + ".open").value(false))
                        .andExpect(jsonPath(day + ".slots.length()").value(0));
            }
        }
    }

    @Test
    @DisplayName("AC-09.2: Default week, with past times excluded")
    void defaultWeekExcludesPastTimes() throws Exception {
        clock.setNow(LocalDateTime.of(2026, 10, 14, 10, 5));

        weekly(D1, S1, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-10-12"))
                .andExpect(jsonPath("$.weekEnd").value("2026-10-18"))
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].open").value(true))
                .andExpect(jsonPath("$.days[0].slots.length()").value(0))
                .andExpect(jsonPath("$.days[1].open").value(true))
                .andExpect(jsonPath("$.days[1].slots.length()").value(0))
                .andExpect(jsonPath("$.days[2].open").value(true))
                .andExpect(jsonPath("$.days[2].slots.length()").value(26))
                .andExpect(jsonPath("$.days[2].slots[0].startTime").value("2026-10-14T10:15:00"))
                .andExpect(jsonPath("$.days[2].slots[0].endTime").value("2026-10-14T10:45:00"))
                .andExpect(jsonPath("$.days[2].slots[25].startTime").value("2026-10-14T16:30:00"))
                .andExpect(jsonPath("$.days[2].slots[25].endTime").value("2026-10-14T17:00:00"))
                .andExpect(jsonPath("$.days[3].slots.length()").value(35))
                .andExpect(jsonPath("$.days[4].slots.length()").value(35))
                .andExpect(jsonPath("$.days[5].slots.length()").value(35));
    }

    @Test
    @DisplayName("AC-09.3: Appointments only affect their own day")
    void appointmentsOnlyAffectOwnDay() throws Exception {
        seeder.seed(T1, B1, tuesday(9, 0), tuesday(10, 30));

        weekly(D1, S2, WEEK_START)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[1].slots.length()").value(21))
                .andExpect(jsonPath("$.days[1].slots[0].startTime").value("2026-10-13T10:30:00"))
                .andExpect(jsonPath("$.days[1].slots[0].endTime").value("2026-10-13T12:00:00"))
                .andExpect(jsonPath("$.days[0].slots.length()").value(31))
                .andExpect(jsonPath("$.days[2].slots.length()").value(31))
                .andExpect(jsonPath("$.days[3].slots.length()").value(31))
                .andExpect(jsonPath("$.days[4].slots.length()").value(31))
                .andExpect(jsonPath("$.days[5].slots.length()").value(31));
    }

    @Test
    @DisplayName("AC-09.4: Same result as the daily view")
    void sameResultAsDailyView() throws Exception {
        seeder.seed(T1, B1, tuesday(9, 0), tuesday(10, 30));
        seeder.seed(T2, B2, LocalDateTime.of(2026, 10, 15, 14, 0), LocalDateTime.of(2026, 10, 15, 15, 0));

        String weeklyBody = weekly(D1, S1, WEEK_START)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (int i = 0; i < 7; i++) {
            String date = LocalDate.parse(WEEK_START).plusDays(i).toString();
            String dailyBody = daily(D1, S1, date)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<Object> weeklySlots = JsonPath.read(weeklyBody, "$.days[" + i + "].slots");
            List<Object> dailySlots = JsonPath.read(dailyBody, "$.slots");

            assertThat(weeklySlots).as("slots for %s", date).isEqualTo(dailySlots);
        }
    }

    @Test
    @DisplayName("AC-09.5: Dealership without the capability")
    void dealershipWithoutCapability() throws Exception {
        ResultActions result = weekly(D2, S3, WEEK_START)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7));

        for (int i = 0; i < 6; i++) {
            result.andExpect(jsonPath("$.days[" + i + "].open").value(true))
                    .andExpect(jsonPath("$.days[" + i + "].slots.length()").value(0));
        }
    }

    static Stream<Arguments> invalidRequests() {
        UUID unknown = UUID.fromString("99999999-9999-9999-9999-999999999999");
        return Stream.of(
                Arguments.of("weekStart is not a Monday", D1, S1, "2026-10-14", 400, "VALIDATION_FAILED", "weekStart"),
                Arguments.of("weekStart has wrong format", D1, S1, "14-10-2026", 400, "VALIDATION_FAILED", null),
                Arguments.of("missing serviceTypeId", D1, null, WEEK_START, 400, "VALIDATION_FAILED", null),
                Arguments.of("unknown dealershipId", unknown, S1, WEEK_START, 404, "DEALERSHIP_NOT_FOUND", null),
                Arguments.of("unknown serviceTypeId", D1, unknown, WEEK_START, 404, "SERVICE_TYPE_NOT_FOUND", null)
        );
    }

    @ParameterizedTest(name = "AC-09.6: {0}")
    @MethodSource("invalidRequests")
    void invalidRequest(String description, UUID dealershipId, UUID serviceTypeId, String weekStart,
                        int expectedStatus, String expectedCode, String expectedErrorField) throws Exception {
        ResultActions result = weekly(dealershipId, serviceTypeId, weekStart)
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));

        if (expectedErrorField != null) {
            result.andExpect(jsonPath("$.errors[0].field").value(expectedErrorField));
        }
    }
}
