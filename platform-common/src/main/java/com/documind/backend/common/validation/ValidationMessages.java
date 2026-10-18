package com.documind.backend.common.validation;

public final class ValidationMessages {

	public static final String VALIDATION_FAILED = "Validation failed";

	public static final String INVALID_VALUE = "Invalid value";
	public static final String REQUIRED = "This parameter is required";
	public static final String UNKNOWN_FIELD = "Unknown field";
	public static final String INVALID = "Invalid";

	public static final String EMAIL_REQUIRED = "Email is required";
	public static final String EMAIL_INVALID = "Enter a valid email address";
	public static final String EMAIL_TOO_LONG = "Email must be at most 254 characters";

	public static final String PASSWORD_REQUIRED = "Password is required";
	public static final String PASSWORD_TOO_SHORT = "Password must be at least 8 characters";
	public static final String PASSWORD_TOO_LONG = "Password must be at most 64 characters";
	public static final String PASSWORD_COMPLEXITY =
			"Password must include at least 1 uppercase letter, 1 lowercase letter, 1 digit, and 1 special character";
	public static final String PASSWORD_COMMON = "This password is too common, please choose a stronger one";
	public static final String PASSWORD_CONTAINS_EMAIL = "Password must not contain your email address";
	public static final String PASSWORD_CONTAINS_NAME = "Password must not contain your full name";

	public static final String FULL_NAME_REQUIRED = "Full name is required";
	public static final String FULL_NAME_TOO_SHORT = "Full name must be at least 2 characters";
	public static final String FULL_NAME_TOO_LONG = "Full name must be at most 100 characters";
	public static final String FULL_NAME_INVALID = "Full name may only contain letters";

	private ValidationMessages() {
	}
}
