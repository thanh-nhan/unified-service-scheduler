// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B2;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C2;
import static com.keyloop.scheduler.support.Fixture.MONDAY;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T2;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V2;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListAppointmentsAcceptanceTest extends IntegrationTest {

    private static final String LIST_URL = "/api/v1/customers/{customerId}/appointments";

    @Test
    @DisplayName("AC-08.1: Only the customer's own appointments")
    void onlyOwnAppointments() throws Exception {
        seeder.seed(T1, B1, at("11:00"), at("11:30"), C1, V1);
        seeder.seed(T1, B1, at("09:00"), at("09:30"), C1, V1);
        seeder.seed(T2, B2, at("10:00"), at("10:30"), C2, V2);

        mockMvc.perform(get(LIST_URL, C1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].startTime").value(MONDAY + "T09:00:00"))
                .andExpect(jsonPath("$.items[1].startTime").value(MONDAY + "T11:00:00"))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("AC-08.2: Status filter")
    void statusFilter() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("09:30"), C1, V1);
        UUID cancelled = seeder.seed(T1, B1, at("10:00"), at("10:30"), C1, V1);
        seeder.cancel(cancelled, at("08:00").minusDays(1));

        mockMvc.perform(get(LIST_URL, C1).param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(cancelled.toString()))
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"));
    }

    @Test
    @DisplayName("AC-08.3: Pagination")
    void pagination() throws Exception {
        for (int i = 0; i < 25; i++) {
            LocalDateTime start = MONDAY.plusDays(i / 5).atTime(9, 0).plusMinutes(30L * (i % 5));
            UUID technician = i % 2 == 0 ? T1 : T2;
            UUID bay = i % 2 == 0 ? B1 : B2;
            seeder.seed(technician, bay, start, start.plusMinutes(30), C1, V1);
        }

        mockMvc.perform(get(LIST_URL, C1).param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @ParameterizedTest(name = "AC-08.4: {0}={1} is rejected")
    @CsvSource({
            "size, 101",
            "size, 0",
            "page, -1"
    })
    void invalidPagingParameters(String parameter, String value) throws Exception {
        mockMvc.perform(get(LIST_URL, C1).param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-08.5: Unknown customer")
    void unknownCustomer() throws Exception {
        mockMvc.perform(get(LIST_URL, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-08.5: Customer with no appointments")
    void customerWithNoAppointments() throws Exception {
        mockMvc.perform(get(LIST_URL, C2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
