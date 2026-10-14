package com.documind.backend.auth.service;

import com.documind.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
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
public class JwtService {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ISSUER = "documind";
    private static final String CLAIM_TYPE = "token_type";
    private static final String TYPE_ACCESS = "access";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Clock clock;
    private final JwtParser accessTokenParser;

    public record AccessToken(String token, String jti, Instant expiresAt, long expiresInSeconds) {
    }

    public record AccessClaims(Long userId, String jti, Instant expiresAt) {
    }

    @Autowired
    public JwtService(JwtProperties properties, Clock clock) {
        this(properties.getSecret(), Duration.ofMinutes(properties.getAccessExpirationMinutes()), clock);
    }

    JwtService(String secret, Duration accessTtl, Clock clock) {
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
        this.accessTokenParser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

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

    public AccessClaims parseAccess(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token must not be blank");
        }

        Claims claims = accessTokenParser
                .parseSignedClaims(token)
                .getPayload();

        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Not an access token");
        }

        return new AccessClaims(
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
}
