package com.documind.auth.auth.validation;

import com.documind.backend.common.validation.ValidationMessages;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 64;

	private static final Pattern COMPLEXITY_PATTERN = Pattern.compile(
			"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,64}$");

	private static final Set<String> COMMON_PASSWORDS = Set.of(
			"password",
			"password123",
			"12345678",
			"123456789",
			"qwerty123",
			"admin123",
			"letmein",
			"welcome1");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		if (value == null || value.isEmpty()) {
			violation(context, ValidationMessages.PASSWORD_REQUIRED);
			return false;
		}
		boolean valid = true;
		if (value.length() < MIN_LENGTH) {
			violation(context, ValidationMessages.PASSWORD_TOO_SHORT);
			valid = false;
		}
		if (value.length() > MAX_LENGTH) {
			violation(context, ValidationMessages.PASSWORD_TOO_LONG);
			valid = false;
		}
		if (!COMPLEXITY_PATTERN.matcher(value).matches()) {
			violation(context, ValidationMessages.PASSWORD_COMPLEXITY);
			valid = false;
		}
		if (COMMON_PASSWORDS.contains(value.toLowerCase(Locale.ROOT))) {
			violation(context, ValidationMessages.PASSWORD_COMMON);
			valid = false;
		}
		return valid;
	}

	private static void violation(ConstraintValidatorContext context, String message) {
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}
