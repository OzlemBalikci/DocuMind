package com.sirket.auth.auth.dto.response;

import java.time.Instant;

public record AuthResponse(
		String tokenType,
		String accessToken,
		Instant accessExpiresAt,
		long accessExpiresIn,
		UserResponse user
) {

	public static final String TOKEN_TYPE_BEARER = "Bearer";
}
