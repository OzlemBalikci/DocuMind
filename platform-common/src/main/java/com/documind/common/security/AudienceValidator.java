package com.documind.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class AudienceValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_AUD = new OAuth2Error("invalid_token", "aud", null);

	private final String audience;

	public AudienceValidator(String audience) {
		if (audience == null || audience.isBlank()) {
			throw new IllegalArgumentException("audience must not be blank");
		}
		this.audience = audience;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		List<String> audiences = jwt.getAudience();
		if (audiences != null && audiences.contains(audience)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(INVALID_AUD);
	}
}
