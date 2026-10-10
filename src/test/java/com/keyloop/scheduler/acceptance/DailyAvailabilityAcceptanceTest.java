// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B2;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.D2;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.S3;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T2;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DailyAvailabilityAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";

    private ResultActions availability(UUID dealershipId, UUID serviceTypeId, String date) throws Exception {
        return mockMvc.perform(get("/api/v1/dealerships/{id}/availability", dealershipId)
                .param("serviceTypeId", serviceTypeId.toString())
                .param("date", date));
    }

    private static String slotWithStart(String start) {
        return "$.slots[?(@.startTime == '" + start + "')]";
    }

    @Test
    @DisplayName("AC-02.1: Full day with no appointments")
    void fullDayNoAppointments() throws Exception {
        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dealershipId").value(D1.toString()))
                .andExpect(jsonPath("$.serviceTypeId").value(S1.toString()))
                .andExpect(jsonPath("$.date").value(MONDAY))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.slots.length()").value(35))
                .andExpect(jsonPath("$.slots[0].startTime").value("2026-10-12T08:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("2026-10-12T08:30:00"))
                .andExpect(jsonPath("$.slots[34].startTime").value("2026-10-12T16:30:00"))
                .andExpect(jsonPath("$.slots[34].endTime").value("2026-10-12T17:00:00"));
    }

    @Test
    @DisplayName("AC-02.2: Only qualified technician is busy")
    void onlyQualifiedTechnicianBusy() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));

        availability(D1, S2, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(90))
                .andExpect(jsonPath("$.slots.length()").value(21))
                .andExpect(jsonPath("$.slots[0].startTime").value("2026-10-12T10:30:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("2026-10-12T12:00:00"))
                .andExpect(jsonPath("$.slots[20].startTime").value("2026-10-12T15:30:00"))
                .andExpect(jsonPath("$.slots[20].endTime").value("2026-10-12T17:00:00"))
                .andExpect(jsonPath(slotWithStart("2026-10-12T08:00:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:00:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T10:15:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T10:30:00")).isNotEmpty());
    }

    @Test
    @DisplayName("AC-02.3: Other resources keep a slot open")
    void otherResourcesKeepSlotOpen() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("09:30"));

        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:00:00")).isNotEmpty());
    }

    @Test
    @DisplayName("AC-02.4: All qualified technicians busy")
    void allQualifiedTechniciansBusy() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("09:30"));
        seeder.seed(T2, B2, at("09:00"), at("09:30"));

        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(slotWithStart("2026-10-12T08:45:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:00:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:15:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T08:30:00")).isNotEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:30:00")).isNotEmpty());
    }

    @Test
    @DisplayName("AC-02.5: Closed day")
    void closedDay() throws Exception {
        availability(D1, S1, "2026-10-18")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    @DisplayName("AC-02.6: Past start times excluded today")
    void pastStartTimesExcludedToday() throws Exception {
        clock.setNow(LocalDateTime.parse("2026-10-12T10:05:00"));

        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].startTime").value("2026-10-12T10:15:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("2026-10-12T10:45:00"))
                .andExpect(jsonPath(slotWithStart("2026-10-12T10:00:00")).isEmpty())
                .andExpect(jsonPath(slotWithStart("2026-10-12T08:00:00")).isEmpty());
    }

    @Test
    @DisplayName("AC-02.7: Past date")
    void pastDate() throws Exception {
        availability(D1, S1, "2026-10-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    @DisplayName("AC-02.8: Dealership without the capability")
    void dealershipWithoutCapability() throws Exception {
        availability(D2, S3, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    @DisplayName("AC-02.9: Cancelled, inactive, and other-dealership resources ignored")
    void cancelledInactiveAndOtherDealershipIgnored() throws Exception {
        LocalDateTime cancelledAt = at("08:00");
        UUID a1 = seeder.seed(T1, B1, at("09:00"), at("09:30"));
        UUID a2 = seeder.seed(T2, B2, at("09:00"), at("09:30"));
        seeder.cancel(a1, cancelledAt);
        seeder.cancel(a2, cancelledAt);

        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:00:00")).isNotEmpty());

        seeder.seed(T1, B1, at("09:00"), at("09:30"));
        seeder.seed(T2, B2, at("09:00"), at("09:30"));

        availability(D1, S1, MONDAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath(slotWithStart("2026-10-12T09:00:00")).isEmpty());
    }

    @Test
    @DisplayName("AC-02.10: Unknown dealershipId returns 404 DEALERSHIP_NOT_FOUND")
    void unknownDealership() throws Exception {
        availability(UUID.randomUUID(), S1, MONDAY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEALERSHIP_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-02.10: Unknown serviceTypeId returns 404 SERVICE_TYPE_NOT_FOUND")
    void unknownServiceType() throws Exception {
        availability(D1, UUID.randomUUID(), MONDAY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_TYPE_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-02.10: Missing serviceTypeId returns 400 VALIDATION_FAILED")
    void missingServiceTypeId() throws Exception {
        mockMvc.perform(get("/api/v1/dealerships/{id}/availability", D1).param("date", MONDAY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-02.10: Missing date returns 400 VALIDATION_FAILED")
    void missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/dealerships/{id}/availability", D1)
                        .param("serviceTypeId", S1.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-02.10: Malformed date returns 400 VALIDATION_FAILED")
    void malformedDate() throws Exception {
        availability(D1, S1, "12-10-2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
