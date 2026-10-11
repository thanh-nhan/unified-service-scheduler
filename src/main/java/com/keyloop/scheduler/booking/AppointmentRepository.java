package com.keyloop.scheduler.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @EntityGraph(attributePaths = {"customer", "vehicle", "dealership", "serviceType", "technician", "serviceBay"})
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") UUID id);
}
