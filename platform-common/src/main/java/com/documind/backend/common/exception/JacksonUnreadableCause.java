package com.documind.backend.common.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Resolves Jackson 3 {@link UnrecognizedPropertyException} from the unreadable-message cause chain.
 */
final class JacksonUnreadableCause {

	private JacksonUnreadableCause() {
	}

	static UnrecognizedPropertyException unrecognizedProperty(HttpMessageNotReadableException ex) {
		Throwable cause = ex.getCause();
		while (cause != null) {
			if (cause instanceof UnrecognizedPropertyException upe) {
				return upe;
			}
			cause = cause.getCause();
		}
		return null;
	}
}
