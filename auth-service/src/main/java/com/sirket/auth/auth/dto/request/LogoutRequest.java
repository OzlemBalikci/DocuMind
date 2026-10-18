package com.sirket.auth.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		String refreshToken,
		@NotBlank String accessToken
) {
}
