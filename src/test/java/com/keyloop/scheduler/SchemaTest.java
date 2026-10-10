package com.keyloop.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class SchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testConstraintsExist() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("SELECT conname FROM pg_constraint");

        assertTrue(constraints.stream().anyMatch(row -> "ex_appointment_technician_overlap".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "ex_appointment_bay_overlap".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "ex_appointment_vehicle_overlap".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "uq_appointment_idempotency".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "fk_appointment_technician".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "fk_appointment_bay".equals(row.get("conname"))));
        assertTrue(constraints.stream().anyMatch(row -> "fk_appointment_vehicle".equals(row.get("conname"))));
    }

    @Test
    void testIndexesExist() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes");

        assertTrue(indexes.stream().anyMatch(row -> "ix_appointment_dealership_start".equals(row.get("indexname"))));
        assertTrue(indexes.stream().anyMatch(row -> "ix_appointment_customer_start".equals(row.get("indexname"))));
    }
}
