package com.keyloop.scheduler.common;

import com.keyloop.scheduler.common.error.GlobalExceptionHandler;
import com.keyloop.scheduler.common.web.RequestIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TestErrorController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class RequestIdTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("AC-E02.1: incoming X-Request-Id is echoed back")
    void echoesIncomingRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/uuid/{id}", UUID.randomUUID())
                .header("X-Request-Id", "abc-123")).andReturn();

        assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo("abc-123");
    }

    @Test
    @DisplayName("AC-E02.1: a UUID is generated when X-Request-Id is absent")
    void generatesRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/uuid/{id}", UUID.randomUUID())).andReturn();

        String header = result.getResponse().getHeader("X-Request-Id");
        assertThat(header).isNotNull();
        assertThat(UUID.fromString(header)).isNotNull();
    }

    @Test
    @DisplayName("AC-E02.1: an invalid incoming X-Request-Id is replaced by a generated UUID")
    void replacesInvalidRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/uuid/{id}", UUID.randomUUID())
                .header("X-Request-Id", "bad id\twith spaces")).andReturn();

        String header = result.getResponse().getHeader("X-Request-Id");
        assertThat(header).isNotEqualTo("bad id\twith spaces");
        assertThat(UUID.fromString(header)).isNotNull();
    }

    @Test
    @DisplayName("AC-E02.2: problem response requestId equals the X-Request-Id header")
    void problemRequestIdMatchesHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/api-exception")
                .header("X-Request-Id", "trace-42"))
                .andExpect(jsonPath("$.requestId").value("trace-42"))
                .andReturn();

        assertThat(result.getResponse().getHeader("X-Request-Id")).isEqualTo("trace-42");
    }

    @Test
    @DisplayName("AC-E02.2: 500 response also carries the requestId")
    void serverErrorCarriesRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/boom")).andReturn();

        String header = result.getResponse().getHeader("X-Request-Id");
        assertThat(header).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains("\"requestId\":\"" + header + "\"");
    }

    @Test
    @DisplayName("AC-E02.2: MDC is cleared after the request")
    void mdcIsCleared() throws Exception {
        mockMvc.perform(get("/test/boom"));

        assertThat(MDC.get("requestId")).isNull();
    }
}
