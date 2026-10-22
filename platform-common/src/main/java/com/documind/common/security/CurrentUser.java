package com.documind.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID id, String email, Set<String> roles) {

	public static CurrentUser from(Jwt jwt) {
		return new CurrentUser(
				UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString("email"),
				KeycloakRoleConverter.realmRoles(jwt));
	}

	public static CurrentUser from(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return from(jwt);
		}
		throw new IllegalStateException("Authenticated principal is not a JWT");
	}

	public boolean hasRole(String role) {
		String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
		return roles.contains(normalized);
	}
}
