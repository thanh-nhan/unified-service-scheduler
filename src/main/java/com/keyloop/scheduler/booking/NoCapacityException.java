package com.keyloop.scheduler.booking;

import com.keyloop.scheduler.common.error.ErrorCode;

/** No technician or bay is free for the requested interval. Not retried (design 04 §4). */
public class NoCapacityException extends RuntimeException {

    private final ErrorCode code;

    public NoCapacityException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
