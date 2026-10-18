package com.documind.auth.token.service.impl;

import com.documind.auth.token.config.JwtProperties;
import com.documind.auth.token.service.TokenService;
import com.documind.common.security.AccessTokenVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

	private static final int MIN_SECRET_LENGTH = 32;
	private static final String ISSUER = "documind";
	private static final String CLAIM_TYPE = "token_type";
	private static final String TYPE_ACCESS = "access";

	private final SecretKey key;
	private final Duration accessTtl;
	private final Clock clock;
	private final AccessTokenVerifier accessTokenVerifier;

	@Autowired
	public TokenServiceImpl(JwtProperties properties, Clock clock) {
		this(properties.getSecret(), Duration.ofMinutes(properties.getAccessExpirationMinutes()), clock);
	}

	TokenServiceImpl(String secret, Duration accessTtl, Clock clock) {
		if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException(
					"app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
		}
		if (accessTtl == null || accessTtl.isZero() || accessTtl.isNegative()) {
			throw new IllegalStateException("access TTL must be positive");
		}
		if (clock == null) {
			throw new IllegalArgumentException("clock must not be null");
		}
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTtl = accessTtl;
		this.clock = clock;
		this.accessTokenVerifier = new AccessTokenVerifier(secret, clock);
	}

	@Override
	public AccessToken issueAccess(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not be null");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = now.plus(accessTtl).truncatedTo(ChronoUnit.SECONDS);
		String jti = UUID.randomUUID().toString();

		String token = Jwts.builder()
				.issuer(ISSUER)
				.subject(userId.toString())
				.id(jti)
				.claim(CLAIM_TYPE, TYPE_ACCESS)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(key, Jwts.SIG.HS256)
				.compact();

		return new AccessToken(token, jti, expiresAt, accessTtl.toSeconds());
	}

	@Override
	public AccessClaims parseAccess(String token) {
		AccessTokenVerifier.AccessTokenClaims claims = accessTokenVerifier.parse(token);
		return new AccessClaims(claims.userId(), claims.jti(), claims.expiresAt());
	}
}
