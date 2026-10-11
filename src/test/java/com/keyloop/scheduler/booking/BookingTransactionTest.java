package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ErrorCode;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B2;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T2;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTransactionTest extends IntegrationTest {

    @Autowired
    private BookingTransaction bookingTransaction;

    private BookingCommand command() {
        var start = at("09:30");
        return new BookingCommand(C1, V1, D1, S1, start, UUID.randomUUID().toString(),
                RequestHasher.hash(C1, V1, D1, S1, start));
    }

    private int appointmentCount() {
        return jdbc.sql("select count(*) from appointment").query(Integer.class).single();
    }

    @Test
    void storesAppointmentWithFirstTechnicianAndBay() {
        Appointment stored = bookingTransaction.tryAllocateAndInsert(command());

        assertThat(stored.getTechnician().getId()).isEqualTo(T1);
        assertThat(stored.getServiceBay().getId()).isEqualTo(B1);
        assertThat(stored.getEndTime()).isEqualTo(at("10:00"));
        assertThat(appointmentCount()).isEqualTo(1);
    }

    @Test
    void throwsWhenNoQualifiedTechnicianIsFree() {
        seeder.seed(T1, B1, at("09:00"), at("10:30"));
        seeder.seed(T2, B2, at("09:00"), at("10:30"));

        assertThatThrownBy(() -> bookingTransaction.tryAllocateAndInsert(command()))
                .isInstanceOfSatisfying(NoCapacityException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.NO_QUALIFIED_TECHNICIAN_AVAILABLE));
        assertThat(appointmentCount()).isEqualTo(2);
    }
}
