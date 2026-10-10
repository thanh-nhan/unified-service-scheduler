package com.keyloop.scheduler;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;

class FoundationTest extends IntegrationTest {

    @Autowired
    private Clock injectedClock;

    @Test
    @DisplayName("AC-E01.4: fixture dataset is loaded")
    void fixtureDatasetIsLoaded() {
        assertThat(count("dealership")).isEqualTo(2);
        assertThat(count("technician")).isEqualTo(5);
    }

    @Test
    @DisplayName("AC-E01.5: extra dealership does not leak between tests (1)")
    void extraDealershipDoesNotLeak1() {
        insertDealership();
        assertThat(count("dealership")).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-E01.5: extra dealership does not leak between tests (2)")
    void extraDealershipDoesNotLeak2() {
        insertDealership();
        assertThat(count("dealership")).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-E01.6: injected clock returns the time set on the test clock")
    void injectedClockIsControllable() {
        LocalDateTime now = at("09:00");
        clock.setNow(now);
        assertThat(LocalDateTime.now(injectedClock)).isEqualTo(now);
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    private void insertDealership() {
        jdbc.sql("INSERT INTO dealership (id, name) VALUES (:id, :name)")
                .param("id", UUID.randomUUID())
                .param("name", "Extra Dealership")
                .update();
    }
}
