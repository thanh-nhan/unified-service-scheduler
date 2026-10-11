package com.keyloop.scheduler.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Customer listing (design 04 §7): one paged query, associations fetched by the entity graph. */
    @EntityGraph(attributePaths = {"customer", "vehicle", "dealership", "serviceType", "technician", "serviceBay"})
    @Query(value = """
            SELECT a FROM Appointment a
            WHERE a.customer.id = :customerId AND (:status IS NULL OR a.status = :status)
            ORDER BY a.startTime, a.id
            """,
            countQuery = """
            SELECT COUNT(a) FROM Appointment a
            WHERE a.customer.id = :customerId AND (:status IS NULL OR a.status = :status)
            """)
    Page<Appointment> findPageForCustomer(@Param("customerId") UUID customerId,
                                          @Param("status") AppointmentStatus status,
                                          Pageable pageable);

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
