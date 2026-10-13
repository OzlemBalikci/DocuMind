package com.documind.backend.auth.dto;

import java.time.Instant;

public record AuthResponse(
		String tokenType,
		String accessToken,
		Instant accessExpiresAt,
		long accessExpiresIn,
		String refreshToken,
		Instant refreshExpiresAt,
		UserResponse user
) {

	public static final String TOKEN_TYPE_BEARER = "Bearer";
}
