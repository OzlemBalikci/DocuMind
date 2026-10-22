package com.documind.common.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestJwtSupport {

	public static final String ISSUER = "http://localhost:8180/realms/documind";
	public static final String AUDIENCE = "documind-api";

	private static final KeyPair KEY_PAIR = rsa();
	private static final RSAKey RSA = new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic())
			.privateKey((RSAPrivateKey) KEY_PAIR.getPrivate())
			.keyID("test")
			.build();

	private TestJwtSupport() {
	}

	public static JwtDecoder decoder() {
		return ResourceServerJwtDecoderFactory.fromPublicKey(
				(RSAPublicKey) KEY_PAIR.getPublic(), ISSUER, AUDIENCE);
	}

	public static String token(UUID subject, String email, String... roles) {
		return token(subject, email, AUDIENCE, Instant.now(), roles);
	}

	public static String token(UUID subject, String email, String audience, Instant authTime, String... roles) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.issuer(ISSUER)
					.subject(subject.toString())
					.audience(audience)
					.issueTime(Date.from(Instant.now()))
					.expirationTime(Date.from(Instant.now().plusSeconds(300)))
					.claim("email", email)
					.claim("name", "Test User")
					.claim("auth_time", authTime.getEpochSecond())
					.claim("realm_access", Map.of("roles", List.of(roles)))
					.build();
			SignedJWT jwt = new SignedJWT(
					new JWSHeader.Builder(JWSAlgorithm.RS256)
							.keyID(RSA.getKeyID())
							.type(JOSEObjectType.JWT)
							.build(),
					claims);
			jwt.sign(new RSASSASigner(RSA));
			return jwt.serialize();
		} catch (Exception ex) {
			throw new IllegalStateException("Could not sign test JWT", ex);
		}
	}

	private static KeyPair rsa() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
