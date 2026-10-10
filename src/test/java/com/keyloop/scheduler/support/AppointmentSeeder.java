package com.keyloop.scheduler.support;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inserts appointments directly into the database, bypassing the API.
 */
public class AppointmentSeeder {

    private static final String REQUEST_HASH = "0".repeat(64);

    private final JdbcClient jdbc;

    public AppointmentSeeder(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID seed(UUID technicianId, UUID bayId, LocalDateTime start, LocalDateTime end) {
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        jdbc.sql("INSERT INTO customer (id, name, email) VALUES (:id, :name, :email)")
                .param("id", customerId)
                .param("name", "Seeded Customer")
                .param("email", customerId + "@seed.example.com")
                .update();
        jdbc.sql("INSERT INTO vehicle (id, customer_id, vin, make, model) VALUES (:id, :customerId, :vin, :make, :model)")
                .param("id", vehicleId)
                .param("customerId", customerId)
                .param("vin", randomVin())
                .param("make", "Seeded")
                .param("model", "Vehicle")
                .update();
        return seed(technicianId, bayId, start, end, customerId, vehicleId);
    }

    public UUID seed(UUID technicianId, UUID bayId, LocalDateTime start, LocalDateTime end,
                     UUID customerId, UUID vehicleId) {
        UUID dealershipId = jdbc.sql("SELECT dealership_id FROM technician WHERE id = :id")
                .param("id", technicianId)
                .query(UUID.class)
                .single();
        UUID appointmentId = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO appointment (id, customer_id, vehicle_id, dealership_id, service_type_id,
                                         technician_id, service_bay_id, start_time, end_time, status,
                                         idempotency_key, request_hash, created_at)
                VALUES (:id, :customerId, :vehicleId, :dealershipId, :serviceTypeId,
                        :technicianId, :bayId, :start, :end, 'CONFIRMED',
                        :idempotencyKey, :requestHash, :createdAt)
                """)
                .param("id", appointmentId)
                .param("customerId", customerId)
                .param("vehicleId", vehicleId)
                .param("dealershipId", dealershipId)
                .param("serviceTypeId", Fixture.S1)
                .param("technicianId", technicianId)
                .param("bayId", bayId)
                .param("start", start)
                .param("end", end)
                .param("idempotencyKey", UUID.randomUUID().toString())
                .param("requestHash", REQUEST_HASH)
                .param("createdAt", start.minusDays(1))
                .update();
        return appointmentId;
    }

    public void cancel(UUID appointmentId, LocalDateTime at) {
        jdbc.sql("UPDATE appointment SET status = 'CANCELLED', cancelled_at = :at WHERE id = :id")
                .param("at", at)
                .param("id", appointmentId)
                .update();
    }

    private static String randomVin() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase();
    }
}
