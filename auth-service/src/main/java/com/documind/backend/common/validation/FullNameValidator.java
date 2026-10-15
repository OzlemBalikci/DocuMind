package com.documind.backend.common.validation;

import com.documind.backend.common.util.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class FullNameValidator implements ConstraintValidator<ValidFullName, String> {

	private static final int MIN_LENGTH = 2;
	private static final int MAX_LENGTH = 100;

	private static final Pattern NAME_PATTERN = Pattern.compile(
			"^[A-Za-zÇĞİÖŞÜçğıöşü]+(?:[ '\\-][A-Za-zÇĞİÖŞÜçğıöşü]+)*$");

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		if (value == null || value.isBlank()) {
			violation(context, ValidationMessages.FULL_NAME_REQUIRED);
			return false;
		}
		String normalized = Texts.normalizeFullName(value);
		if (normalized.isEmpty()) {
			violation(context, ValidationMessages.FULL_NAME_REQUIRED);
			return false;
		}
		boolean valid = true;
		if (normalized.length() < MIN_LENGTH) {
			violation(context, ValidationMessages.FULL_NAME_TOO_SHORT);
			valid = false;
		}
		if (normalized.length() > MAX_LENGTH) {
			violation(context, ValidationMessages.FULL_NAME_TOO_LONG);
			valid = false;
		}
		if (!NAME_PATTERN.matcher(normalized).matches()) {
			violation(context, ValidationMessages.FULL_NAME_INVALID);
			valid = false;
		}
		return valid;
	}

	private static void violation(ConstraintValidatorContext context, String message) {
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}
