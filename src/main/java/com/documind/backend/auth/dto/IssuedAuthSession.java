package com.documind.backend.auth.dto;

import java.time.Instant;

/**
 * API response plus refresh token material for HttpOnly cookie (not exposed in JSON).
 */
public record IssuedAuthSession(
		AuthResponse response,
		String refreshToken,
		Instant refreshExpiresAt
) {
}
