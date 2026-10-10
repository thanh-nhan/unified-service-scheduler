package com.keyloop.scheduler.common;

import com.keyloop.scheduler.common.error.ApiException;
import com.keyloop.scheduler.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TestErrorController {

    public record Body(@NotBlank String name) {
    }

    @GetMapping("/test/api-exception")
    void apiException() {
        throw new ApiException(ErrorCode.NO_QUALIFIED_TECHNICIAN_AVAILABLE);
    }

    @GetMapping("/test/api-exception/{code}")
    void apiExceptionWithCode(@PathVariable ErrorCode code) {
        throw new ApiException(code);
    }

    @PostMapping("/test/body")
    void body(@Valid @RequestBody Body body) {
    }

    @GetMapping("/test/uuid/{id}")
    String uuid(@PathVariable UUID id) {
        return id.toString();
    }

    @GetMapping("/test/param")
    String param(@RequestParam String q) {
        return q;
    }

    @GetMapping("/test/min")
    String min(@RequestParam @Min(1) int n) {
        return String.valueOf(n);
    }

    @GetMapping("/test/idempotency")
    String idempotency(@RequestHeader("Idempotency-Key") String key) {
        return key;
    }

    @GetMapping("/test/other-header")
    String otherHeader(@RequestHeader("X-Other") String value) {
        return value;
    }

    @GetMapping("/test/boom")
    void boom() {
        throw new IllegalStateException("secret internal detail");
    }
}
