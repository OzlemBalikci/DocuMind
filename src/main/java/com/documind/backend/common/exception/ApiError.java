package com.documind.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError (
        String code,
        String message,
        Instant timestamp,
        Map<String, String> details
){
    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message, Instant.now(), null);
    }

    public static ApiError of(ErrorCode code, String message, Map<String, String> details) {
        return new ApiError(code.name(), message, Instant.now(), details);
    }

}
