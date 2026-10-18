package com.documind.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public final class AccessTokenVerifier {

	private static final int MIN_SECRET_LENGTH = 32;
	private static final String ISSUER = "documind";
	private static final String CLAIM_TYPE = "token_type";
	private static final String TYPE_ACCESS = "access";

	private final JwtParser parser;

	public AccessTokenVerifier(String secret, Clock clock) {
		if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException(
					"app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
		}
		if (clock == null) {
			throw new IllegalArgumentException("clock must not be null");
		}
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.parser = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(ISSUER)
				.clock(() -> Date.from(clock.instant()))
				.build();
	}

	public AccessTokenClaims parse(String token) {
		if (token == null || token.isBlank()) {
			throw new JwtException("Token must not be blank");
		}
		Claims claims = parser.parseSignedClaims(token).getPayload();
		if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new JwtException("Not an access token");
		}
		return new AccessTokenClaims(
				parseUserId(claims.getSubject()),
				claims.getId(),
				claims.getExpiration().toInstant().truncatedTo(ChronoUnit.SECONDS));
	}

	private static Long parseUserId(String subject) {
		if (subject == null || subject.isBlank()) {
			throw new JwtException("Token subject must not be blank");
		}
		try {
			return Long.valueOf(subject);
		} catch (NumberFormatException ex) {
			throw new JwtException("Token subject is not a valid user id", ex);
		}
	}

	public record AccessTokenClaims(Long userId, String jti, Instant expiresAt) {
	}
}
