package com.documind.backend.common.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextsTest {

	@Test
	void normalizeEmail_trimsAndLowercasesWithRootLocale() {
		assertEquals("ali@test.com", Texts.normalizeEmail("  Ali@test.COM  "));
	}

	@Test
	void normalizeEmail_usesRootNotDefaultLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertEquals("i@test.com", Texts.normalizeEmail("I@test.com"));
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void normalizeEmail_rejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> Texts.normalizeEmail(null));
	}
}
