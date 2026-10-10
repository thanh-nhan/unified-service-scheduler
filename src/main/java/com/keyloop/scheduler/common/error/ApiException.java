package com.keyloop.scheduler.common.error;

import java.util.List;

public class ApiException extends RuntimeException {

    public record FieldError(String field, String message) {
    }

    private final ErrorCode code;
    private final transient List<FieldError> errors;

    public ApiException(ErrorCode code) {
        this(code, null, List.of());
    }

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, List.of());
    }

    public ApiException(ErrorCode code, String detail, List<FieldError> errors) {
        super(detail != null ? detail : code.defaultDetail());
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public ErrorCode code() {
        return code;
    }

    public List<FieldError> errors() {
        return errors;
    }
}
