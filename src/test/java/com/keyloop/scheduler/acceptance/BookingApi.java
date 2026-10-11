package com.keyloop.scheduler.acceptance;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Helper for booking tests: POST /api/v1/appointments.
 */
public class BookingApi {

    private final MockMvc mockMvc;

    public BookingApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public ResultActions book(UUID customer, UUID vehicle, UUID dealership, UUID serviceType, String startTime)
            throws Exception {
        return book(UUID.randomUUID().toString(), customer, vehicle, dealership, serviceType, startTime);
    }

    public ResultActions book(String idempotencyKey, UUID customer, UUID vehicle, UUID dealership,
                              UUID serviceType, String startTime) throws Exception {
        String body = """
                {"customerId":"%s","vehicleId":"%s","dealershipId":"%s","serviceTypeId":"%s","startTime":"%s"}
                """.formatted(customer, vehicle, dealership, serviceType, startTime);
        return mockMvc.perform(post("/api/v1/appointments")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
