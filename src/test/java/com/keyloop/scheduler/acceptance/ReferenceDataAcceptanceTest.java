// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.D2;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.S3;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDataAcceptanceTest extends IntegrationTest {

    private static final String[] WEEKDAYS =
            {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

    @Test
    @DisplayName("AC-01.1: List dealerships")
    void listDealerships() throws Exception {
        var result = mockMvc.perform(get("/api/v1/dealerships"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(D1.toString()))
                .andExpect(jsonPath("$[1].id").value(D2.toString()))
                .andExpect(jsonPath("$[0].openingHours.length()").value(6));

        for (int i = 0; i < WEEKDAYS.length; i++) {
            String entry = "$[0].openingHours[" + i + "]";
            result.andExpect(jsonPath(entry + ".dayOfWeek").value(WEEKDAYS[i]))
                    .andExpect(jsonPath(entry + ".opensAt").value("08:00"))
                    .andExpect(jsonPath(entry + ".closesAt").value("17:00"));
        }
    }

    @Test
    @DisplayName("AC-01.2: List service types")
    void listServiceTypes() throws Exception {
        mockMvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(S2.toString()))
                .andExpect(jsonPath("$[0].name").value("Brake Service"))
                .andExpect(jsonPath("$[0].durationMinutes").value(90))
                .andExpect(jsonPath("$[1].id").value(S3.toString()))
                .andExpect(jsonPath("$[1].name").value("EV Battery Check"))
                .andExpect(jsonPath("$[1].durationMinutes").value(60))
                .andExpect(jsonPath("$[2].id").value(S1.toString()))
                .andExpect(jsonPath("$[2].name").value("Oil Change"))
                .andExpect(jsonPath("$[2].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].code").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$[0].requiredSkill").doesNotExist())
                .andExpect(jsonPath("$[0].requiredBayType").doesNotExist())
                .andExpect(jsonPath("$[1].requiredSkill").doesNotExist())
                .andExpect(jsonPath("$[1].requiredBayType").doesNotExist())
                .andExpect(jsonPath("$[2].requiredSkill").doesNotExist())
                .andExpect(jsonPath("$[2].requiredBayType").doesNotExist());
    }

    @Test
    @DisplayName("AC-01.3: Empty catalogue")
    void emptyCatalogue() throws Exception {
        jdbc.sql("DELETE FROM service_type").update();

        mockMvc.perform(get("/api/v1/service-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
