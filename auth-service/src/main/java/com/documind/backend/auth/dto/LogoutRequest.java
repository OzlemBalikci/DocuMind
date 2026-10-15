package com.documind.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
		String refreshToken,
		@NotBlank String accessToken
) {
}
