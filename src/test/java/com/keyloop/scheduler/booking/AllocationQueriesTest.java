package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.catalog.BayType;
import com.keyloop.scheduler.catalog.Skill;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.*;
import static org.assertj.core.api.Assertions.assertThat;

class AllocationQueriesTest extends IntegrationTest {

    @Autowired
    private AllocationQueries allocation;

    @Test
    void noAppointmentsPicksLowestIds() {
        assertThat(allocation.bestTechnician(D1, Skill.GENERAL_MAINTENANCE, at("09:30"), at("10:00")))
                .contains(T1);
        assertThat(allocation.lowestFreeBay(D1, BayType.GENERAL, at("09:30"), at("10:00"))).contains(B1);
    }

    @Test
    void busyResourcesAreSkipped() {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));

        assertThat(allocation.bestTechnician(D1, Skill.GENERAL_MAINTENANCE, at("09:30"), at("10:00")))
                .contains(T2);
        assertThat(allocation.lowestFreeBay(D1, BayType.GENERAL, at("09:30"), at("10:00"))).contains(B2);
    }

    @Test
    void leastLoadedTechnicianWinsAndFreeBayIsLowestId() {
        seeder.seed(T1, B1, at("08:00"), at("08:30"));

        assertThat(allocation.bestTechnician(D1, Skill.GENERAL_MAINTENANCE, at("11:00"), at("11:30")))
                .contains(T2);
        assertThat(allocation.lowestFreeBay(D1, BayType.GENERAL, at("11:00"), at("11:30"))).contains(B1);
    }

    @Test
    void noQualifiedTechnicianFreeReturnsEmpty() {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));

        assertThat(allocation.bestTechnician(D1, Skill.BRAKES, at("09:30"), at("11:00"))).isEmpty();
    }

    @Test
    void inactiveAndOtherDealershipTechniciansAreNeverReturned() {
        seeder.seed(T1, B1, at("09:00"), at("10:00"));
        seeder.seed(T2, B2, at("09:00"), at("10:00"));

        // T3 lacks the skill, T4 is inactive, T5 belongs to D2.
        assertThat(allocation.bestTechnician(D1, Skill.GENERAL_MAINTENANCE, at("09:00"), at("09:30")))
                .isEmpty();
        assertThat(allocation.bestTechnician(D2, Skill.GENERAL_MAINTENANCE, at("09:00"), at("09:30")))
                .contains(T5);
    }

    @Test
    void backToBackAppointmentsDoNotOverlap() {
        seeder.seed(T1, B1, at("08:30"), at("10:00"));

        assertThat(allocation.bestTechnician(D1, Skill.BRAKES, at("10:00"), at("11:30"))).contains(T1);
        assertThat(allocation.lowestFreeBay(D1, BayType.GENERAL, at("10:00"), at("11:30"))).contains(B1);
    }

    @Test
    void cancelledAppointmentDoesNotBlock() {
        UUID id = seeder.seed(T1, B1, at("09:00"), at("10:30"));
        seeder.cancel(id, at("08:00"));

        assertThat(allocation.bestTechnician(D1, Skill.BRAKES, at("09:30"), at("10:00"))).contains(T1);
        assertThat(allocation.lowestFreeBay(D1, BayType.GENERAL, at("09:30"), at("10:00"))).contains(B1);
    }

    @Test
    void vehicleOverlapIsDetected() {
        seeder.seed(T1, B1, at("09:00"), at("10:00"), C1, V1);

        assertThat(allocation.vehicleHasOverlap(V1, at("09:30"), at("10:30"))).isTrue();
        assertThat(allocation.vehicleHasOverlap(V1, at("10:00"), at("10:30"))).isFalse();
        assertThat(allocation.vehicleHasOverlap(V2, at("09:30"), at("10:30"))).isFalse();
    }
}
