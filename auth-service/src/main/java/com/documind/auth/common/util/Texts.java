package com.documind.auth.common.util;

import java.util.Locale;

public final class Texts {

	private Texts() {
	}

	public static String normalizeEmail(String email) {
		if (email == null) {
			throw new IllegalArgumentException("email must not be null");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	public static String normalizeFullName(String fullName) {
		if (fullName == null) {
			throw new IllegalArgumentException("fullName must not be null");
		}
		return fullName.trim().replaceAll("\\s+", " ");
	}

	public static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max);
	}

}
