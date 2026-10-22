package com.documind.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		return realmRoles(jwt).stream()
				.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toUnmodifiableSet());
	}

	@SuppressWarnings("unchecked")
	static Set<String> realmRoles(Jwt jwt) {
		Map<String, Object> realmAccess = jwt.getClaim("realm_access");
		if (realmAccess == null) {
			return Set.of();
		}
		Object roles = realmAccess.get("roles");
		if (!(roles instanceof Collection<?> roleNames)) {
			return Set.of();
		}
		return roleNames.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.collect(Collectors.toUnmodifiableSet());
	}
}
