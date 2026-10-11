// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B2;
import static com.keyloop.scheduler.support.Fixture.B3;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C2;
import static com.keyloop.scheduler.support.Fixture.C3;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.S3;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T2;
import static com.keyloop.scheduler.support.Fixture.T3;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V2;
import static com.keyloop.scheduler.support.Fixture.V3;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookAppointmentAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";

    private BookingApi api;

    @BeforeEach
    void setUpApi() {
        api = new BookingApi(mockMvc);
    }

    private int appointmentCount() {
        return jdbc.sql("select count(*) from appointment").query(Integer.class).single();
    }

    @Test
    @DisplayName("AC-03.1: Successful booking")
    void successfulBooking() throws Exception {
        assertThat(appointmentCount()).isZero();

        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/appointments/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startTime").value(MONDAY + "T09:30:00"))
                .andExpect(jsonPath("$.endTime").value(MONDAY + "T10:00:00"))
                .andExpect(jsonPath("$.technician.id").value(T1.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B1.toString()));

        assertThat(appointmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-03.2: Busy technician is avoided")
    void busyTechnicianAvoided() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));

        api.book(C2, V2, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T2.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B2.toString()));
    }

    @Test
    @DisplayName("AC-03.3: Least-loaded technician is chosen")
    void leastLoadedTechnicianChosen() throws Exception {
        seeder.seed(T1, B1, at("08:00"), at("08:30"));

        api.book(C2, V2, D1, S1, MONDAY + "T11:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T2.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B1.toString()));
    }

    @Test
    @DisplayName("AC-03.4: Required skill beats workload")
    void requiredSkillBeatsWorkload() throws Exception {
        seeder.seed(T1, B1, at("08:00"), at("08:30"));

        api.book(C2, V2, D1, S2, MONDAY + "T11:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T1.toString()));
    }

    @Test
    @DisplayName("AC-03.5: No qualified technician")
    void noQualifiedTechnician() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        int before = appointmentCount();

        api.book(C2, V2, D1, S2, MONDAY + "T09:30:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_QUALIFIED_TECHNICIAN_AVAILABLE"))
                .andExpect(jsonPath("$.detail").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.alternatives").doesNotExist());

        assertThat(appointmentCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("AC-03.6: No bay available")
    void noBayAvailable() throws Exception {
        UUID t6 = UUID.fromString("20000000-0000-0000-0000-000000000006");
        jdbc.sql("INSERT INTO technician (id, dealership_id, name, active) VALUES (:id, :dealershipId, :name, true)")
                .param("id", t6)
                .param("dealershipId", D1)
                .param("name", "Extra Technician")
                .update();
        jdbc.sql("INSERT INTO technician_skill (technician_id, skill) VALUES (:id, 'GENERAL_MAINTENANCE')")
                .param("id", t6)
                .update();
        seeder.seed(T1, B1, at("09:30"), at("10:00"));
        seeder.seed(T2, B2, at("09:30"), at("10:00"));
        int before = appointmentCount();

        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SERVICE_BAY_AVAILABLE"));

        assertThat(appointmentCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("AC-03.7: Back-to-back is allowed")
    void backToBackAllowed() throws Exception {
        seeder.seed(T1, B1, at("08:30"), at("10:00"));

        api.book(C2, V2, D1, S2, MONDAY + "T10:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T1.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B1.toString()));
    }

    @Test
    @DisplayName("AC-03.8: Inactive and other-dealership resources are never used")
    void inactiveAndOtherDealershipNeverUsed() throws Exception {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        seeder.seed(T2, B2, at("09:00"), at("10:30"));
        int before = appointmentCount();

        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_QUALIFIED_TECHNICIAN_AVAILABLE"));

        assertThat(appointmentCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("AC-03.9: Bay type is respected")
    void bayTypeRespected() throws Exception {
        api.book(C3, V3, D1, S3, MONDAY + "T14:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T3.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B3.toString()));
    }

    @Test
    @DisplayName("AC-03.10: Vehicle cannot be double-booked")
    void vehicleCannotBeDoubleBooked() throws Exception {
        seeder.seed(T2, B2, at("09:00"), at("09:30"), C1, V1);
        int before = appointmentCount();

        api.book(C1, V1, D1, S2, MONDAY + "T09:15:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ALREADY_BOOKED"));

        assertThat(appointmentCount()).isEqualTo(before);
    }
}
