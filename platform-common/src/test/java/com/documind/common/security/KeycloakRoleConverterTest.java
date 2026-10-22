package com.documind.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

	private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

	@Test
	void mapsRealmRolesToSpringAuthorities() {
		Jwt jwt = jwt(Map.of("roles", List.of("USER", "ADMIN")));
		assertThat(converter.convert(jwt))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
	}

	@Test
	void emptyWhenRealmAccessMissing() {
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "none")
				.subject("sub")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		assertThat(converter.convert(jwt)).isEmpty();
	}

	private static Jwt jwt(Map<String, Object> realmAccess) {
		return Jwt.withTokenValue("t")
				.header("alg", "none")
				.subject("11111111-1111-1111-1111-111111111111")
				.claim("realm_access", realmAccess)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
	}
}
