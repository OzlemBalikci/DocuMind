package com.documind.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

	private final AudienceValidator validator = new AudienceValidator("documind-api");

	@Test
	void acceptsExpectedAudience() {
		OAuth2TokenValidatorResult result = validator.validate(jwt(List.of("documind-api")));
		assertThat(result.hasErrors()).isFalse();
	}

	@Test
	void rejectsWrongAudience() {
		OAuth2TokenValidatorResult result = validator.validate(jwt(List.of("other-client")));
		assertThat(result.hasErrors()).isTrue();
	}

	private static Jwt jwt(List<String> audience) {
		return Jwt.withTokenValue("t")
				.header("alg", "none")
				.subject("11111111-1111-1111-1111-111111111111")
				.audience(audience)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}
}
