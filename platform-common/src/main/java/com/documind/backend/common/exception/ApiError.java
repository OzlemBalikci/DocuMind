package com.documind.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
		int status,
		String code,
		String message,
		String path,
		Instant timestamp,
		Map<String, String> details
) {
	public static ApiError of(ErrorCode errorCode, String message, String path) {
		return new ApiError(
				errorCode.getStatus().value(),
				errorCode.name(),
				message,
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				null);
	}

	public static ApiError of(ErrorCode errorCode, String message, String path, Map<String, String> details) {
		return new ApiError(
				errorCode.getStatus().value(),
				errorCode.name(),
				message,
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				details);
	}

	public static ApiError ofStatus(int httpStatus, String message, String path) {
		return new ApiError(
				httpStatus,
				"HTTP_" + httpStatus,
				message,
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				null);
	}
}
