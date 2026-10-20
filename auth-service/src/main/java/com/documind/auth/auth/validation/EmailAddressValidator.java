package com.documind.auth.auth.validation;

import com.documind.backend.common.validation.ValidationMessages;

import com.documind.auth.common.util.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailAddressValidator implements ConstraintValidator<ValidEmail, String> {

	private static final int MAX_TOTAL_LENGTH = 254;
	private static final int MAX_LOCAL_LENGTH = 64;
	private static final int MIN_TOTAL_LENGTH = 5;

	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		if (value == null || value.isBlank()) {
			addViolation(context, ValidationMessages.EMAIL_REQUIRED);
			return false;
		}
		String normalized = Texts.normalizeEmail(value);
		if (normalized.length() < MIN_TOTAL_LENGTH) {
			addViolation(context, ValidationMessages.EMAIL_INVALID);
			return false;
		}
		if (normalized.length() > MAX_TOTAL_LENGTH) {
			addViolation(context, ValidationMessages.EMAIL_TOO_LONG);
			return false;
		}
		int at = normalized.indexOf('@');
		if (at < 0) {
			addViolation(context, ValidationMessages.EMAIL_INVALID);
			return false;
		}
		String local = normalized.substring(0, at);
		if (local.length() > MAX_LOCAL_LENGTH) {
			addViolation(context, ValidationMessages.EMAIL_INVALID);
			return false;
		}
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			addViolation(context, ValidationMessages.EMAIL_INVALID);
			return false;
		}
		return true;
	}

	private static void addViolation(ConstraintValidatorContext context, String message) {
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}
