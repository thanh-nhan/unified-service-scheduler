package com.keyloop.scheduler.availability;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class AvailabilityQueries {

    /** A CONFIRMED appointment's resources and interval. */
    public record BusyRow(UUID technicianId, UUID bayId, LocalDateTime start, LocalDateTime end) {
    }

    private final JdbcClient jdbc;

    public AvailabilityQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Active technicians of the dealership who have the required skill. */
    public Set<UUID> qualifiedTechnicianIds(UUID dealershipId, String skill) {
        return Set.copyOf(jdbc.sql("""
                        SELECT t.id FROM technician t
                        JOIN technician_skill s ON s.technician_id = t.id
                        WHERE t.dealership_id = :dealershipId AND t.active AND s.skill = :skill
                        """)
                .param("dealershipId", dealershipId)
                .param("skill", skill)
                .query(UUID.class)
                .list());
    }

    /** Active bays of the dealership with the required type. */
    public Set<UUID> eligibleBayIds(UUID dealershipId, String bayType) {
        return Set.copyOf(jdbc.sql("""
                        SELECT id FROM service_bay
                        WHERE dealership_id = :dealershipId AND active AND bay_type = :bayType
                        """)
                .param("dealershipId", dealershipId)
                .param("bayType", bayType)
                .query(UUID.class)
                .list());
    }

    /** CONFIRMED appointments overlapping [from, to) that use any of the given technicians or bays. */
    public List<BusyRow> busyIntervals(Set<UUID> technicianIds, Set<UUID> bayIds,
                                       LocalDateTime from, LocalDateTime to) {
        if (technicianIds.isEmpty() && bayIds.isEmpty()) {
            return List.of();
        }
        List<String> resourceConditions = new ArrayList<>();
        if (!technicianIds.isEmpty()) {
            resourceConditions.add("technician_id IN (:techIds)");
        }
        if (!bayIds.isEmpty()) {
            resourceConditions.add("service_bay_id IN (:bayIds)");
        }
        String sql = """
                SELECT technician_id, service_bay_id, start_time, end_time
                FROM appointment
                WHERE status = 'CONFIRMED' AND start_time < :to AND end_time > :from
                  AND (%s)
                """.formatted(String.join(" OR ", resourceConditions));
        JdbcClient.StatementSpec spec = jdbc.sql(sql).param("from", from).param("to", to);
        if (!technicianIds.isEmpty()) {
            spec = spec.param("techIds", technicianIds);
        }
        if (!bayIds.isEmpty()) {
            spec = spec.param("bayIds", bayIds);
        }
        return spec.query((rs, i) -> new BusyRow(
                rs.getObject("technician_id", UUID.class),
                rs.getObject("service_bay_id", UUID.class),
                rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class))).list();
    }
}
