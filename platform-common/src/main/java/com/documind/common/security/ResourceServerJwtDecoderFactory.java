package com.documind.common.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

import java.security.interfaces.RSAPublicKey;

public final class ResourceServerJwtDecoderFactory {

	private ResourceServerJwtDecoderFactory() {
	}

	public static JwtDecoder fromIssuerOrJwkSet(String issuerUri, String jwkSetUri, String audience) {
		if (issuerUri == null || issuerUri.isBlank()) {
			throw new IllegalStateException("spring.security.oauth2.resourceserver.jwt.issuer-uri must be set");
		}
		if (jwkSetUri != null && !jwkSetUri.isBlank()) {
			return nimbus(NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build(), issuerUri, audience);
		}
		return new SupplierJwtDecoder(() ->
				nimbus((NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri), issuerUri, audience));
	}

	public static JwtDecoder fromPublicKey(RSAPublicKey publicKey, String issuerUri, String audience) {
		return nimbus(NimbusJwtDecoder.withPublicKey(publicKey).build(), issuerUri, audience);
	}

	private static NimbusJwtDecoder nimbus(NimbusJwtDecoder decoder, String issuerUri, String audience) {
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuerUri),
				new AudienceValidator(audience));
		decoder.setJwtValidator(validator);
		return decoder;
	}
}
