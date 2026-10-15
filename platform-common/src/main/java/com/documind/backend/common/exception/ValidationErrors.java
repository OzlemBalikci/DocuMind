package com.documind.backend.common.exception;

import com.documind.backend.common.validation.ValidationMessages;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ValidationErrors {

	private ValidationErrors() {
	}

	public static List<ValidationErrorResponse.FieldViolation> fromBindingResult(BindingResult bindingResult) {
		List<ValidationErrorResponse.FieldViolation> errors = new ArrayList<>();
		for (FieldError fieldError : bindingResult.getFieldErrors()) {
			errors.add(new ValidationErrorResponse.FieldViolation(
					fieldError.getField(),
					message(fieldError.getDefaultMessage())));
		}
		for (ObjectError globalError : bindingResult.getGlobalErrors()) {
			errors.add(new ValidationErrorResponse.FieldViolation(
					globalError.getObjectName(),
					message(globalError.getDefaultMessage())));
		}
		return errors;
	}

	public static ValidationErrorResponse response(BindingResult bindingResult) {
		return ValidationErrorResponse.of(
				ValidationMessages.VALIDATION_FAILED,
				fromBindingResult(bindingResult));
	}

	static String message(String defaultMessage) {
		return Objects.requireNonNullElse(defaultMessage, ValidationMessages.INVALID);
	}
}
