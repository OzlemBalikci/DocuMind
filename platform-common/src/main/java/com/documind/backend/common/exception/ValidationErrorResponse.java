package com.documind.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ValidationErrorResponse(
		boolean success,
		String message,
		List<FieldViolation> errors
) {

	public static ValidationErrorResponse of(String message, List<FieldViolation> errors) {
		return new ValidationErrorResponse(false, message, errors);
	}

	public record FieldViolation(String field, String message) {
	}
}
