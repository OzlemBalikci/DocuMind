package com.documind.backend.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-minimum-32-characters-long";

    private static JwtService service(Clock clock) {
        return new JwtService(SECRET, Duration.ofMinutes(15), clock);
    }

    @Test
    void rejectsShortSecret() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short", Duration.ofMinutes(15), clock));
    }

    @Test
    void roundTripPreservesUserIdAndJti() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService jwt = service(clock);

        JwtService.AccessToken issued = jwt.issueAccess(42L);
        JwtService.AccessClaims claims = jwt.parseAccess(issued.token());

        assertEquals(42L, claims.userId());
        assertEquals(issued.jti(), claims.jti());
        assertEquals(issued.expiresAt(), claims.expiresAt());
    }

    @Test
    void expiresAtIsFifteenMinutesAfterClockInstant() {
        Instant fixed = Instant.parse("2025-06-01T12:00:00Z");
        Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);
        JwtService jwt = service(clock);

        JwtService.AccessToken issued = jwt.issueAccess(1L);

        assertEquals(fixed.plus(Duration.ofMinutes(15)), issued.expiresAt());
    }

    @Test
    void eachIssueGetsDistinctJti() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService jwt = service(clock);

        String jti1 = jwt.issueAccess(1L).jti();
        String jti2 = jwt.issueAccess(1L).jti();

        assertNotEquals(jti1, jti2);
    }

    @Test
    void rejectsBlankToken() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService jwt = service(clock);
        assertThrows(JwtException.class, () -> jwt.parseAccess("   "));
    }

    @Test
    void rejectsTokenSignedWithOtherSecret() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService issuer = service(clock);
        JwtService other = new JwtService(
                "another-secret-at-least-32-chars-long!!",
                Duration.ofMinutes(15),
                clock);

        String token = issuer.issueAccess(1L).token();
        assertThrows(JwtException.class, () -> other.parseAccess(token));
    }

    @Test
    void rejectsExpiredToken() {
        Instant issuedAt = Instant.parse("2025-06-01T12:00:00Z");
        JwtService issuer = service(Clock.fixed(issuedAt, ZoneOffset.UTC));
        JwtService later = service(Clock.fixed(issuedAt.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        String token = issuer.issueAccess(1L).token();
        assertThrows(ExpiredJwtException.class, () -> later.parseAccess(token));
    }

    @Test
    void acceptsTokenJustBeforeExpiry() {
        Instant issuedAt = Instant.parse("2025-06-01T12:00:00Z");
        JwtService issuer = service(Clock.fixed(issuedAt, ZoneOffset.UTC));
        JwtService later = service(Clock.fixed(issuedAt.plus(Duration.ofMinutes(14)), ZoneOffset.UTC));

        String token = issuer.issueAccess(1L).token();
        assertEquals(1L, later.parseAccess(token).userId());
    }

    @Test
    void rejectsNonPositiveTtl() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        assertThrows(IllegalStateException.class,
                () -> new JwtService(SECRET, Duration.ofMinutes(-1), clock));
    }

    @Test
    void rejectsNullUserIdOnIssue() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService jwt = service(clock);
        assertThrows(IllegalArgumentException.class, () -> jwt.issueAccess(null));
    }

    @Test
    void parseReturnsNonNullClaims() {
        Clock clock = Clock.fixed(Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);
        JwtService jwt = service(clock);
        JwtService.AccessToken issued = jwt.issueAccess(7L);
        assertNotNull(jwt.parseAccess(issued.token()));
    }
}
