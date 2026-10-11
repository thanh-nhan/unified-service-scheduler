// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.jayway.jsonpath.JsonPath;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ViewAppointmentAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";

    private BookingApi api;

    @BeforeEach
    void setUpApi() {
        api = new BookingApi(mockMvc);
    }

    @Test
    @DisplayName("AC-06.1: View a confirmed appointment")
    void viewConfirmedAppointment() throws Exception {
        String bookingBody = api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> booked = JsonPath.read(bookingBody, "$");
        String id = (String) booked.get("id");

        String viewBody = mockMvc.perform(get("/api/v1/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value(MONDAY + "T09:30:00"))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> viewed = JsonPath.read(viewBody, "$");

        assertThat(viewed.keySet()).containsExactlyInAnyOrderElementsOf(booked.keySet());
        for (Map.Entry<String, Object> field : booked.entrySet()) {
            assertThat(viewed.get(field.getKey())).as("field %s", field.getKey()).isEqualTo(field.getValue());
        }
    }

    @Test
    @DisplayName("AC-06.2: View a cancelled appointment")
    void viewCancelledAppointment() throws Exception {
        UUID id = seeder.seed(T1, B1, at("09:30"), at("10:00"));
        seeder.cancel(id, at("08:00").minusDays(1));

        mockMvc.perform(get("/api/v1/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("AC-06.3: Unknown appointment")
    void unknownAppointment() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-06.4: Malformed id")
    void malformedId() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-06.5: No contact details exposed")
    void noContactDetailsExposed() throws Exception {
        String bookingBody = api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(bookingBody, "$.id");

        String body = mockMvc.perform(get("/api/v1/appointments/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"email\"").doesNotContain("\"phone\"");
    }
}
