package com.documind.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
		String code,
		String message,
		int status,
		String path,
		Instant timestamp,
		Map<String, String> details
) {
	public static ApiError of(ErrorCode errorCode, String message, String path) {
		return new ApiError(
				errorCode.name(),
				message,
				errorCode.getStatus().value(),
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				null);
	}

	public static ApiError of(ErrorCode errorCode, String message, String path, Map<String, String> details) {
		return new ApiError(
				errorCode.name(),
				message,
				errorCode.getStatus().value(),
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				details);
	}

	public static ApiError ofStatus(int httpStatus, String message, String path) {
		return new ApiError(
				"HTTP_" + httpStatus,
				message,
				httpStatus,
				path,
				Instant.now().truncatedTo(ChronoUnit.SECONDS),
				null);
	}
}
