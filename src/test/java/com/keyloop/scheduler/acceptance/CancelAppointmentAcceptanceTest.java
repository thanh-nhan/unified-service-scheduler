// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.jayway.jsonpath.JsonPath;
import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.keyloop.scheduler.support.Fixture.B1;
import static com.keyloop.scheduler.support.Fixture.B3;
import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.C3;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S3;
import static com.keyloop.scheduler.support.Fixture.T1;
import static com.keyloop.scheduler.support.Fixture.T3;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V3;
import static com.keyloop.scheduler.support.Fixture.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelAppointmentAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";
    private static final String NOW = "2026-10-11T10:00:00";

    private BookingApi api;

    @BeforeEach
    void setUpApi() {
        api = new BookingApi(mockMvc);
    }

    private ResultActions cancel(Object id) throws Exception {
        return mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id));
    }

    private String book(UUID customer, UUID vehicle, UUID serviceType, String start) throws Exception {
        String body = api.book(customer, vehicle, D1, serviceType, start)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String statusOf(UUID id) {
        return jdbc.sql("select status from appointment where id = :id")
                .param("id", id).query(String.class).single();
    }

    @Test
    @DisplayName("AC-07.1: Cancel a future appointment")
    void cancelFutureAppointment() throws Exception {
        String id = book(C1, V1, S1, MONDAY + "T09:30:00");

        cancel(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(NOW));
    }

    @Test
    @DisplayName("AC-07.2: Cancellation frees resources")
    void cancellationFreesResources() throws Exception {
        String id = book(C3, V3, S3, MONDAY + "T09:00:00");
        cancel(id).andExpect(status().isOk());

        api.book(C3, V3, D1, S3, MONDAY + "T09:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.technician.id").value(T3.toString()))
                .andExpect(jsonPath("$.serviceBay.id").value(B3.toString()));
    }

    @Test
    @DisplayName("AC-07.3: Availability reflects the cancellation")
    void availabilityReflectsCancellation() throws Exception {
        UUID id = seeder.seed(T3, B3, at("09:00"), at("10:00"));
        String slot = "$.slots[?(@.startTime == '" + MONDAY + "T09:00:00')]";

        mockMvc.perform(get("/api/v1/dealerships/{id}/availability", D1)
                        .param("serviceTypeId", S3.toString()).param("date", MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath(slot).isEmpty());

        cancel(id).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dealerships/{id}/availability", D1)
                        .param("serviceTypeId", S3.toString()).param("date", MONDAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath(slot).isNotEmpty());
    }

    @Test
    @DisplayName("AC-07.4: Cancelling twice")
    void cancellingTwice() throws Exception {
        String id = book(C1, V1, S1, MONDAY + "T09:30:00");
        cancel(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledAt").value(NOW));

        clock.setNow(LocalDateTime.of(2026, 10, 11, 11, 0));

        cancel(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value(NOW));
    }

    @Test
    @DisplayName("AC-07.5: Started appointments cannot be cancelled")
    void startedAppointmentsCannotBeCancelled() throws Exception {
        UUID id = seeder.seed(T1, B1, at("09:00"), at("09:30"));
        clock.setNow(at("09:00"));

        cancel(id)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("APPOINTMENT_ALREADY_STARTED"));

        assertThat(statusOf(id)).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("AC-07.6: Unknown appointment")
    void unknownAppointment() throws Exception {
        cancel(UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-07.7: Concurrent cancellations")
    void concurrentCancellations() throws Exception {
        String id = book(C1, V1, S1, MONDAY + "T09:30:00");

        List<MvcResult> results = runConcurrently(List.of(
                () -> cancel(id).andReturn(),
                () -> cancel(id).andReturn()));

        for (MvcResult result : results) {
            String body = result.getResponse().getContentAsString();
            assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);
            assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("CANCELLED");
            assertThat((String) JsonPath.read(body, "$.cancelledAt")).isEqualTo(NOW);
        }
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
}
