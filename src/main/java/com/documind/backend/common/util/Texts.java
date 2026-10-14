package com.documind.backend.common.util;

import java.util.Locale;

public final class Texts {

	private Texts() {
	}

	/**
	 * Canonical form for storing and looking up user emails (trim + {@link Locale#ROOT} lower case).
	 */
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
