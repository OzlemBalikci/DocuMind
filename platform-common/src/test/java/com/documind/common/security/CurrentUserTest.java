package com.documind.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

	@Test
	void readsSubjectEmailAndRoles() {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "none")
				.subject(id.toString())
				.claim("email", "user@example.com")
				.claim("realm_access", Map.of("roles", List.of("USER")))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		CurrentUser user = CurrentUser.from(jwt);
		assertThat(user.id()).isEqualTo(id);
		assertThat(user.email()).isEqualTo("user@example.com");
		assertThat(user.hasRole("USER")).isTrue();
		assertThat(user.hasRole("ADMIN")).isFalse();
	}
}
