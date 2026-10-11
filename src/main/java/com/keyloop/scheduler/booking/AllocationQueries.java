package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.catalog.BayType;
import com.keyloop.scheduler.catalog.Skill;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Native resource-selection queries (design 04 §3). UUID ordering is left to PostgreSQL. */
@Repository
public class AllocationQueries {

    private final JdbcClient jdbc;

    public AllocationQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Free for the whole interval, least booked minutes that day, then lowest id. */
    public Optional<UUID> bestTechnician(UUID dealershipId, Skill skill, LocalDateTime start, LocalDateTime end) {
        LocalDateTime dayStart = start.toLocalDate().atStartOfDay();
        return jdbc.sql("""
                        SELECT t.id
                        FROM technician t
                        JOIN technician_skill ts ON ts.technician_id = t.id AND ts.skill = :requiredSkill
                        WHERE t.dealership_id = :dealershipId
                          AND t.active
                          AND NOT EXISTS (
                                SELECT 1 FROM appointment a
                                WHERE a.technician_id = t.id
                                  AND a.status = 'CONFIRMED'
                                  AND a.time_range && tsrange(:start, :end, '[)'))
                        ORDER BY (
                                SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (a.end_time - a.start_time))), 0)
                                FROM appointment a
                                WHERE a.technician_id = t.id
                                  AND a.status = 'CONFIRMED'
                                  AND a.start_time >= :dayStart AND a.start_time < :dayEnd) ASC,
                              t.id ASC
                        LIMIT 1
                        """)
                .param("requiredSkill", skill.name())
                .param("dealershipId", dealershipId)
                .param("start", start)
                .param("end", end)
                .param("dayStart", dayStart)
                .param("dayEnd", dayStart.plusDays(1))
                .query(UUID.class)
                .optional();
    }

    /** Lowest-id eligible bay that is free for the whole interval. */
    public Optional<UUID> lowestFreeBay(UUID dealershipId, BayType bayType, LocalDateTime start, LocalDateTime end) {
        return jdbc.sql("""
                        SELECT b.id
                        FROM service_bay b
                        WHERE b.dealership_id = :dealershipId
                          AND b.bay_type = :requiredBayType
                          AND b.active
                          AND NOT EXISTS (
                                SELECT 1 FROM appointment a
                                WHERE a.service_bay_id = b.id
                                  AND a.status = 'CONFIRMED'
                                  AND a.time_range && tsrange(:start, :end, '[)'))
                        ORDER BY b.id ASC
                        LIMIT 1
                        """)
                .param("dealershipId", dealershipId)
                .param("requiredBayType", bayType.name())
                .param("start", start)
                .param("end", end)
                .query(UUID.class)
                .optional();
    }

    public boolean vehicleHasOverlap(UUID vehicleId, LocalDateTime start, LocalDateTime end) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM appointment a
                            WHERE a.vehicle_id = :vehicleId
                              AND a.status = 'CONFIRMED'
                              AND a.time_range && tsrange(:start, :end, '[)'))
                        """)
                .param("vehicleId", vehicleId)
                .param("start", start)
                .param("end", end)
                .query(Boolean.class)
                .single();
    }
}
