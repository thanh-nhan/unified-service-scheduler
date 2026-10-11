package com.keyloop.scheduler.acceptance;

import com.jayway.jsonpath.JsonPath;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C2;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.S3;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T3;
import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V2;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;

class ReliableBookingAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";

    private BookingApi booking;

    @BeforeEach
    void setUpApi() {
        booking = new BookingApi(mockMvc);
    }

    private int appointmentCount() {
        return jdbc.sql("select count(*) from appointment").query(Integer.class).single();
    }

    /** Runs all tasks at the same moment (released by a single latch) and returns the results in task order. */
    private static List<MvcResult> runConcurrently(List<Callable<MvcResult>> tasks) throws Exception {
        int n = tasks.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch ready = new CountDownLatch(n);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (Callable<MvcResult> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }
            ready.await();
            go.countDown();
            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private static String content(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    private static String jsonString(MvcResult result, String path) throws Exception {
        return JsonPath.read(content(result), path);
    }

    @Test
    @DisplayName("AC-05.1: Concurrent requests for the last slot")
    void concurrentRequestsForLastSlot() throws Exception {
        int n = 20;
        UUID[] customers = new UUID[n];
        UUID[] vehicles = new UUID[n];
        for (int i = 0; i < n; i++) {
            customers[i] = UUID.randomUUID();
            vehicles[i] = UUID.randomUUID();
            jdbc.sql("INSERT INTO customer (id, name, email) VALUES (:id, :name, :email)")
                    .param("id", customers[i])
                    .param("name", "Concurrent Customer " + i)
                    .param("email", "concurrent" + i + "@example.com")
                    .update();
            jdbc.sql("INSERT INTO vehicle (id, customer_id, vin, make, model) "
                            + "VALUES (:id, :customerId, :vin, :make, :model)")
                    .param("id", vehicles[i])
                    .param("customerId", customers[i])
                    .param("vin", UUID.randomUUID().toString().replace("-", "").substring(0, 17).toUpperCase())
                    .param("make", "Test")
                    .param("model", "EV")
                    .update();
        }

        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int idx = i;
            tasks.add(() -> booking.book(customers[idx], vehicles[idx], D1, S3, MONDAY + "T09:00:00")
                    .andReturn());
        }
        List<MvcResult> results = runConcurrently(tasks);

        List<Integer> statuses = results.stream().map(ReliableBookingAcceptanceTest::status).toList();
        assertThat(statuses).noneMatch(s -> s >= 500);
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(n - 1);
        for (MvcResult result : results) {
            if (status(result) == 409) {
                assertThat(jsonString(result, "$.code")).isIn(
                        "NO_QUALIFIED_TECHNICIAN_AVAILABLE", "NO_SERVICE_BAY_AVAILABLE", "HIGH_CONTENTION");
            }
        }

        int overlapping = jdbc.sql("""
                        select count(*) from appointment
                        where technician_id = :technicianId and status = 'CONFIRMED'
                          and start_time < :end and end_time > :start
                        """)
                .param("technicianId", T3)
                .param("start", at("09:00"))
                .param("end", at("10:00"))
                .query(Integer.class)
                .single();
        assertThat(overlapping).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-05.2: A lost race is resolved by retrying with another resource")
    void lostRaceResolvedByRetry() throws Exception {
        String[] starts = {"08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30"};

        for (String start : starts) {
            String startTime = MONDAY + "T" + start + ":00";
            List<MvcResult> results = runConcurrently(List.of(
                    () -> booking.book(C1, V1, D1, S1, startTime).andReturn(),
                    () -> booking.book(C2, V2, D1, S1, startTime).andReturn()));

            assertThat(status(results.get(0))).as("C1 at %s", start).isEqualTo(201);
            assertThat(status(results.get(1))).as("C2 at %s", start).isEqualTo(201);
            assertThat(jsonString(results.get(0), "$.technician.id"))
                    .as("technicians at %s", start)
                    .isNotEqualTo(jsonString(results.get(1), "$.technician.id"));
            assertThat(jsonString(results.get(0), "$.serviceBay.id"))
                    .as("bays at %s", start)
                    .isNotEqualTo(jsonString(results.get(1), "$.serviceBay.id"));
        }
    }

    @Test
    @DisplayName("AC-05.3: Idempotent replay")
    void idempotentReplay() throws Exception {
        MvcResult first = booking.book("K1", C1, V1, D1, S1, MONDAY + "T09:30:00").andReturn();
        assertThat(status(first)).isEqualTo(201);

        MvcResult replay = booking.book("K1", C1, V1, D1, S1, MONDAY + "T09:30:00").andReturn();

        assertThat(status(replay)).isEqualTo(200);
        assertThat(jsonString(replay, "$.id")).isEqualTo(jsonString(first, "$.id"));
        assertThat(content(replay)).isEqualTo(content(first));
        assertThat(appointmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-05.4: Key reused with a different body")
    void keyReusedWithDifferentBody() throws Exception {
        MvcResult first = booking.book("K1", C1, V1, D1, S1, MONDAY + "T09:30:00").andReturn();
        assertThat(status(first)).isEqualTo(201);

        MvcResult reused = booking.book("K1", C1, V1, D1, S1, MONDAY + "T11:00:00").andReturn();

        assertThat(status(reused)).isEqualTo(422);
        assertThat(jsonString(reused, "$.code")).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(appointmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-05.5: Concurrent duplicates")
    void concurrentDuplicates() throws Exception {
        List<Callable<MvcResult>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> booking.book("K2", C1, V1, D1, S1, MONDAY + "T09:30:00").andReturn());
        }
        List<MvcResult> results = runConcurrently(tasks);

        for (MvcResult result : results) {
            assertThat(status(result)).isIn(200, 201);
        }
        assertThat(results.stream().map(r -> {
            try {
                return jsonString(r, "$.id");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).distinct()).hasSize(1);
        assertThat(appointmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-05.6: A rejected request can be retried")
    void rejectedRequestCanBeRetried() throws Exception {
        UUID blocking = seeder.seed(T1, B1, at("09:00"), at("10:30"));

        MvcResult rejected = booking.book("K3", C2, V2, D1, S2, MONDAY + "T09:30:00").andReturn();
        assertThat(status(rejected)).isEqualTo(409);
        int before = appointmentCount();

        seeder.cancel(blocking, at("08:00"));

        MvcResult retried = booking.book("K3", C2, V2, D1, S2, MONDAY + "T09:30:00").andReturn();
        assertThat(status(retried)).isEqualTo(201);
        assertThat(appointmentCount()).isEqualTo(before + 1);
    }
}
