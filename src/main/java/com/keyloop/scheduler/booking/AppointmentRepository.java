package com.keyloop.scheduler.booking;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @EntityGraph(attributePaths = {"customer", "vehicle", "dealership", "serviceType", "technician", "serviceBay"})
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") UUID id);

    /** Conditional cancel (design 04 §6): returns the number of rows updated, 0 or 1. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Appointment a SET a.status = com.keyloop.scheduler.booking.AppointmentStatus.CANCELLED,
                                     a.cancelledAt = :now
            WHERE a.id = :id AND a.status = com.keyloop.scheduler.booking.AppointmentStatus.CONFIRMED
              AND a.startTime > :now
            """)
    int cancelIfFuture(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
