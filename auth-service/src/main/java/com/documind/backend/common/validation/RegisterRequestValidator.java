package com.documind.backend.common.validation;

import com.documind.backend.auth.dto.RegisterRequest;
import com.documind.backend.common.util.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class RegisterRequestValidator implements ConstraintValidator<ValidRegisterRequest, RegisterRequest> {

	private static final int MIN_TOKEN_LENGTH = 3;

	@Override
	public boolean isValid(RegisterRequest value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		String password = value.password();
		String email = value.email();
		String fullName = value.fullName();
		if (password == null || password.isEmpty() || email == null || email.isBlank()) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		String passwordLower = password.toLowerCase(Locale.ROOT);
		boolean valid = true;

		try {
			String normalizedEmail = Texts.normalizeEmail(email);
			int at = normalizedEmail.indexOf('@');
			if (at > 0) {
				String localPart = normalizedEmail.substring(0, at);
				if (localPart.length() >= MIN_TOKEN_LENGTH
						&& passwordLower.contains(localPart.toLowerCase(Locale.ROOT))) {
					addPasswordViolation(context, ValidationMessages.PASSWORD_CONTAINS_EMAIL);
					valid = false;
				}
			}
		} catch (IllegalArgumentException ignored) {
			return true;
		}

		if (fullName != null && !fullName.isBlank()) {
			String normalizedName = Texts.normalizeFullName(fullName);
			for (String part : normalizedName.split(" ")) {
				if (part.length() < MIN_TOKEN_LENGTH) {
					continue;
				}
				String token = part.toLowerCase(Locale.ROOT);
				if (passwordLower.contains(token)) {
					addPasswordViolation(context, ValidationMessages.PASSWORD_CONTAINS_NAME);
					valid = false;
					break;
				}
			}
		}
		return valid;
	}

	private static void addPasswordViolation(ConstraintValidatorContext context, String message) {
		context.buildConstraintViolationWithTemplate(message)
				.addPropertyNode("password")
				.addConstraintViolation();
	}
}
