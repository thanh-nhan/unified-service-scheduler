// FROZEN: approved by EM 2026-10-11
package com.keyloop.scheduler.acceptance;

import com.keyloop.scheduler.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static com.keyloop.scheduler.support.Fixture.C1;
import static com.keyloop.scheduler.support.Fixture.D1;
import static com.keyloop.scheduler.support.Fixture.S1;
import static com.keyloop.scheduler.support.Fixture.S2;
import static com.keyloop.scheduler.support.Fixture.V1;
import static com.keyloop.scheduler.support.Fixture.V2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingValidationAcceptanceTest extends IntegrationTest {

    private static final String MONDAY = "2026-10-12";
    private static final String SUNDAY = "2026-10-18";
    private static final String UNKNOWN_ID = "99999999-9999-9999-9999-999999999999";

    private BookingApi api;

    @BeforeEach
    void setUpApi() {
        api = new BookingApi(mockMvc);
    }

    private int appointmentCount() {
        return jdbc.sql("select count(*) from appointment").query(Integer.class).single();
    }

    private static String body(Object customer, Object vehicle, Object dealership, Object serviceType,
                               String startTime) {
        return """
                {"customerId":"%s","vehicleId":"%s","dealershipId":"%s","serviceTypeId":"%s","startTime":"%s"}
                """.formatted(customer, vehicle, dealership, serviceType, startTime);
    }

    /** Posts a raw body; a null idempotency key means the header is not sent at all. */
    private ResultActions postRaw(String idempotencyKey, String body) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request);
    }

    @Test
    @DisplayName("AC-04.1: Missing field")
    void missingField() throws Exception {
        String body = """
                {"customerId":"%s","vehicleId":"%s","dealershipId":"%s","startTime":"%s"}
                """.formatted(C1, V1, D1, MONDAY + "T09:30:00");

        postRaw(UUID.randomUUID().toString(), body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'serviceTypeId')]").isNotEmpty());
    }

    @ParameterizedTest(name = "AC-04.2: startTime {0}")
    @CsvSource({"2026-10-12T09:30:00+07:00", "2026-10-12T02:30:00Z"})
    @DisplayName("AC-04.2: Timestamp with an offset")
    void timestampWithOffset(String startTime) throws Exception {
        api.book(C1, V1, D1, S1, startTime)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'startTime')]").isNotEmpty());
    }

    @Test
    @DisplayName("AC-04.3: Malformed JSON")
    void malformedJson() throws Exception {
        postRaw(UUID.randomUUID().toString(), "{\"customerId\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("AC-04.4: Missing idempotency key")
    void missingIdempotencyKey() throws Exception {
        postRaw(null, body(C1, V1, D1, S1, MONDAY + "T09:30:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_MISSING"));
    }

    @ParameterizedTest(name = "AC-04.5: unknown {0} -> {1}")
    @CsvSource({
            "customerId,CUSTOMER_NOT_FOUND",
            "vehicleId,VEHICLE_NOT_FOUND",
            "dealershipId,DEALERSHIP_NOT_FOUND",
            "serviceTypeId,SERVICE_TYPE_NOT_FOUND"
    })
    @DisplayName("AC-04.5: Unknown references")
    void unknownReferences(String unknownField, String expectedCode) throws Exception {
        Object customer = unknownField.equals("customerId") ? UNKNOWN_ID : C1;
        Object vehicle = unknownField.equals("vehicleId") ? UNKNOWN_ID : V1;
        Object dealership = unknownField.equals("dealershipId") ? UNKNOWN_ID : D1;
        Object serviceType = unknownField.equals("serviceTypeId") ? UNKNOWN_ID : S1;

        postRaw(UUID.randomUUID().toString(),
                body(customer, vehicle, dealership, serviceType, MONDAY + "T09:30:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    @DisplayName("AC-04.6: Vehicle not owned by customer")
    void vehicleNotOwnedByCustomer() throws Exception {
        api.book(C1, V2, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_OWNED_BY_CUSTOMER"));
    }

    @Test
    @DisplayName("AC-04.7: Start time in the past")
    void startTimeInPast() throws Exception {
        clock.setNow(LocalDateTime.parse(MONDAY + "T09:30:00"));

        api.book(C1, V1, D1, S1, MONDAY + "T09:30:00")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("START_TIME_IN_PAST"));

        api.book(C1, V1, D1, S1, MONDAY + "T09:45:00")
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "AC-04.8: startTime {0}")
    @CsvSource({"2026-10-12T09:10:00", "2026-10-12T09:15:30"})
    @DisplayName("AC-04.8: Start time not on the grid")
    void startTimeNotOnGrid(String startTime) throws Exception {
        api.book(C1, V1, D1, S1, startTime)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("START_TIME_NOT_ALIGNED"));
    }

    @ParameterizedTest(name = "AC-04.9: {0} at {1} -> {2}")
    @CsvSource({
            "OIL_CHANGE,2026-10-12T07:45:00,422",
            "OIL_CHANGE,2026-10-12T16:45:00,422",
            "BRAKE_SERVICE,2026-10-12T16:00:00,422",
            "OIL_CHANGE,2026-10-18T10:00:00,422",
            "OIL_CHANGE,2026-10-12T08:00:00,201",
            "OIL_CHANGE,2026-10-12T16:30:00,201"
    })
    @DisplayName("AC-04.9: Opening hours")
    void openingHours(String service, String startTime, int expectedStatus) throws Exception {
        UUID serviceType = service.equals("OIL_CHANGE") ? S1 : S2;

        ResultActions result = api.book(C1, V1, D1, serviceType, startTime)
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 422) {
            result.andExpect(jsonPath("$.code").value("OUTSIDE_OPENING_HOURS"));
        }
    }

    @Test
    @DisplayName("AC-04.10: Check order")
    void checkOrder() throws Exception {
        api.book(C1, V1, D1, S1, "2026-10-10T09:10:00")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("START_TIME_IN_PAST"));
    }

    record Rejected(String name, String idempotencyKey, String body) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> rejectedRequests() {
        String key = "rejected-request-key";
        return Stream.of(
                new Rejected("AC-04.1 missing field", key, """
                        {"customerId":"%s","vehicleId":"%s","dealershipId":"%s","startTime":"%s"}
                        """.formatted(C1, V1, D1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.2 offset", key, body(C1, V1, D1, S1, MONDAY + "T09:30:00+07:00")),
                new Rejected("AC-04.3 malformed JSON", key, "{\"customerId\":"),
                new Rejected("AC-04.4 missing key", null, body(C1, V1, D1, S1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.5 unknown customer", key,
                        body(UNKNOWN_ID, V1, D1, S1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.5 unknown vehicle", key,
                        body(C1, UNKNOWN_ID, D1, S1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.5 unknown dealership", key,
                        body(C1, V1, UNKNOWN_ID, S1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.5 unknown service type", key,
                        body(C1, V1, D1, UNKNOWN_ID, MONDAY + "T09:30:00")),
                new Rejected("AC-04.6 vehicle not owned", key, body(C1, V2, D1, S1, MONDAY + "T09:30:00")),
                new Rejected("AC-04.7 in the past", key, body(C1, V1, D1, S1, "2026-10-10T09:00:00")),
                new Rejected("AC-04.8 off the grid", key, body(C1, V1, D1, S1, MONDAY + "T09:10:00")),
                new Rejected("AC-04.9 outside opening hours", key, body(C1, V1, D1, S1, SUNDAY + "T10:00:00")),
                new Rejected("AC-04.10 past and off the grid", key, body(C1, V1, D1, S1, "2026-10-10T09:10:00"))
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "AC-04.11: {0}")
    @MethodSource("rejectedRequests")
    @DisplayName("AC-04.11: No side effects on rejection")
    void noSideEffectsOnRejection(Rejected rejected) throws Exception {
        int before = appointmentCount();

        postRaw(rejected.idempotencyKey(), rejected.body())
                .andExpect(status().is4xxClientError());

        assertThat(appointmentCount()).isEqualTo(before);
    }
}
