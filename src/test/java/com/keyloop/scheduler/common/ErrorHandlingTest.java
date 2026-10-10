package com.keyloop.scheduler.common;

import com.keyloop.scheduler.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestErrorController.class)
@Import(GlobalExceptionHandler.class)
class ErrorHandlingTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("ApiException maps to its code's status, code and default detail")
    void apiException() throws Exception {
        mockMvc.perform(get("/test/api-exception"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NO_QUALIFIED_TECHNICIAN_AVAILABLE"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "No technician is available for the selected time. Please choose another time."));
    }

    @Test
    @DisplayName("Every ErrorCode maps to its own status")
    void everyCodeUsesItsOwnStatus() throws Exception {
        mockMvc.perform(get("/test/api-exception/VEHICLE_NOT_FOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        mockMvc.perform(get("/test/api-exception/START_TIME_IN_PAST"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("START_TIME_IN_PAST"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException maps to 400 VALIDATION_FAILED with errors")
    void bodyValidation() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("HandlerMethodValidationException maps to 400 VALIDATION_FAILED with errors")
    void methodValidation() throws Exception {
        mockMvc.perform(get("/test/min").param("n", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("n"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    @DisplayName("Unreadable JSON maps to 400 VALIDATION_FAILED")
    void badJson() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Malformed UUID in path maps to 400 VALIDATION_FAILED with errors entry")
    void malformedUuid() throws Exception {
        mockMvc.perform(get("/test/uuid/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    @DisplayName("Missing request parameter maps to 400 VALIDATION_FAILED with errors entry")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    @DisplayName("Missing Idempotency-Key header maps to 400 IDEMPOTENCY_KEY_MISSING")
    void missingIdempotencyKey() throws Exception {
        mockMvc.perform(get("/test/idempotency"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_MISSING"));
    }

    @Test
    @DisplayName("Other missing header maps to 400 VALIDATION_FAILED")
    void missingOtherHeader() throws Exception {
        mockMvc.perform(get("/test/other-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Unexpected exception maps to 500 with generic detail and no internal message")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret"))));
    }
}
