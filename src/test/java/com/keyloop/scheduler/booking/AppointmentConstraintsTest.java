package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.persistence.ConstraintViolations;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AppointmentConstraintsTest extends IntegrationTest {

    @Test
    void overlappingTechnicianIsExclusionViolation() {
        seeder.seed(T1, B1, at("09:00"), at("10:00"), C1, V1);

        Throwable thrown = catchThrowable(() -> insert(C2, V2, T1, B2, "k1", at("09:30"), at("10:30")));

        assertExclusion(thrown, "ex_appointment_technician_overlap");
    }

    @Test
    void overlappingBayIsExclusionViolation() {
        seeder.seed(T1, B1, at("09:00"), at("10:00"), C1, V1);

        Throwable thrown = catchThrowable(() -> insert(C2, V2, T2, B1, "k1", at("09:30"), at("10:30")));

        assertExclusion(thrown, "ex_appointment_bay_overlap");
    }

    @Test
    void overlappingVehicleIsExclusionViolation() {
        seeder.seed(T1, B1, at("09:00"), at("10:00"), C1, V1);

        Throwable thrown = catchThrowable(() -> insert(C1, V1, T2, B2, "k1", at("09:30"), at("10:30")));

        assertExclusion(thrown, "ex_appointment_vehicle_overlap");
    }

    @Test
    void duplicateIdempotencyKeyIsUniqueViolation() {
        insert(C1, V1, T1, B1, "same-key", at("09:00"), at("10:00"));

        Throwable thrown = catchThrowable(() -> insert(C1, V1, T2, B2, "same-key", at("11:00"), at("12:00")));

        assertThat(ConstraintViolations.isUniqueViolation(thrown)).isTrue();
        assertThat(ConstraintViolations.isExclusionViolation(thrown)).isFalse();
        assertThat(ConstraintViolations.constraintName(thrown)).contains("uq_appointment_idempotency");
    }

    private void assertExclusion(Throwable thrown, String constraint) {
        assertThat(thrown).isNotNull();
        assertThat(ConstraintViolations.isExclusionViolation(thrown)).isTrue();
        assertThat(ConstraintViolations.isUniqueViolation(thrown)).isFalse();
        assertThat(ConstraintViolations.constraintName(thrown)).contains(constraint);
    }

    private void insert(UUID customerId, UUID vehicleId, UUID technicianId, UUID bayId, String key,
                        LocalDateTime start, LocalDateTime end) {
        jdbc.sql("""
                        INSERT INTO appointment (id, customer_id, vehicle_id, dealership_id, service_type_id,
                                                 technician_id, service_bay_id, start_time, end_time, status,
                                                 idempotency_key, request_hash, created_at)
                        VALUES (:id, :customerId, :vehicleId, :dealershipId, :serviceTypeId,
                                :technicianId, :bayId, :start, :end, 'CONFIRMED',
                                :key, :hash, :createdAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("customerId", customerId)
                .param("vehicleId", vehicleId)
                .param("dealershipId", D1)
                .param("serviceTypeId", S1)
                .param("technicianId", technicianId)
                .param("bayId", bayId)
                .param("start", start)
                .param("end", end)
                .param("key", key)
                .param("hash", "0".repeat(64))
                .param("createdAt", start.minusDays(1))
                .update();
    }
}
