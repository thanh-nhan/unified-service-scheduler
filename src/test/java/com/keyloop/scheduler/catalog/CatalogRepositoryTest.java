package com.keyloop.scheduler.catalog;

import com.keyloop.scheduler.customer.Vehicle;
import com.keyloop.scheduler.customer.VehicleRepository;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C2;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T4;
import static com.keyloop.scheduler.support.Fixture.V2;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogRepositoryTest extends IntegrationTest {

    @Autowired
    DealershipRepository dealerships;

    @Autowired
    TechnicianRepository technicians;

    @Autowired
    VehicleRepository vehicles;

    @Test
    @Transactional
    void dealershipHasSixOpeningHoursMondayToSaturday() {
        Dealership d1 = dealerships.findById(D1).orElseThrow();

        List<OpeningHours> hours = d1.getOpeningHours().stream()
                .sorted(Comparator.comparing(OpeningHours::getDayOfWeek))
                .toList();

        assertThat(hours).hasSize(6);
        assertThat(hours).extracting(OpeningHours::getDayOfWeek)
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        assertThat(hours).allSatisfy(h -> {
            assertThat(h.getOpensAt()).isEqualTo(LocalTime.of(8, 0));
            assertThat(h.getClosesAt()).isEqualTo(LocalTime.of(17, 0));
        });
    }

    @Test
    @Transactional
    void technicianSkillsAndActiveFlag() {
        Technician t1 = technicians.findById(T1).orElseThrow();
        Technician t4 = technicians.findById(T4).orElseThrow();

        assertThat(t1.getSkills()).containsExactlyInAnyOrder(Skill.GENERAL_MAINTENANCE, Skill.BRAKES);
        assertThat(t4.isActive()).isFalse();
    }

    @Test
    @Transactional
    void vehicleOwnership() {
        Vehicle v2 = vehicles.findById(V2).orElseThrow();

        assertThat(v2.belongsTo(C2)).isTrue();
        assertThat(v2.belongsTo(C1)).isFalse();
    }
}
