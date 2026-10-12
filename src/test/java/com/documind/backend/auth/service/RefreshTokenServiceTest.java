package com.documind.backend.auth.service;

import com.documind.backend.auth.entity.RefreshToken;
import com.documind.backend.auth.repository.RefreshTokenRepository;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.user.entity.User;
import com.documind.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=test-jwt-secret-minimum-32-characters-long",
        "app.jwt.access-expiration-minutes=15",
        "app.jwt.refresh-expiration-days=7"
})
class RefreshTokenServiceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16"));

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("user-" + UUID.randomUUID() + "@documind.test")
                .passwordHash("bcrypt-placeholder")
                .fullName("Test User")
                .build());
    }

    @Test
    void issue_persistsHashAndReturnsRawToken() {
        RefreshTokenService.IssuedRefresh issued = refreshTokenService.issue(user, "JUnit", "127.0.0.1");

        assertNotNull(issued.rawToken());
        assertTrue(issued.expiresAt().isAfter(Instant.now()));

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(issued.rawToken()))
                .orElseThrow();
        assertEquals(user.getId(), stored.getUser().getId());
        assertNull(stored.getRevokedAt());
    }

    @Test
    void rotate_issuesNewTokenAndRevokesPrevious() {
        RefreshTokenService.IssuedRefresh first = refreshTokenService.issue(user, "JUnit", "127.0.0.1");

        RefreshTokenService.RotatedRefresh rotated = refreshTokenService.rotate(
                first.rawToken(), "JUnit", "10.0.0.2");

        assertEquals(user.getId(), rotated.user().getId());
        assertNotEquals(first.rawToken(), rotated.refresh().rawToken());

        RefreshToken oldRow = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(first.rawToken()))
                .orElseThrow();
        assertNotNull(oldRow.getRevokedAt());
        assertNotNull(oldRow.getReplacedBy());

        RefreshToken newRow = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(rotated.refresh().rawToken()))
                .orElseThrow();
        assertNull(newRow.getRevokedAt());
        assertEquals(oldRow.getFamilyId(), newRow.getFamilyId());
    }

    @Test
    void reuse_revokesEntireFamily() {
        RefreshTokenService.IssuedRefresh first = refreshTokenService.issue(user, "JUnit", "127.0.0.1");
        RefreshTokenService.RotatedRefresh rotated = refreshTokenService.rotate(
                first.rawToken(), "JUnit", "127.0.0.1");

        UUID familyId = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(first.rawToken()))
                .orElseThrow()
                .getFamilyId();

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(first.rawToken(), "JUnit", "127.0.0.1"));

        List<RefreshToken> activeInFamily = refreshTokenRepository.findByFamilyIdAndRevokedAtIsNull(familyId);
        assertTrue(activeInFamily.isEmpty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(rotated.refresh().rawToken(), "JUnit", "127.0.0.1"));
    }

    @Test
    void rotate_unknownToken_throws() {
        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate("not-a-real-token", "JUnit", "127.0.0.1"));
    }

    @Test
    void rotate_expiredToken_throwsAndRevokesRow() {
        RefreshTokenService.IssuedRefresh issued = refreshTokenService.issue(user, "JUnit", "127.0.0.1");
        String hash = RefreshTokenService.hash(issued.rawToken());
        jdbcTemplate.update(
                "UPDATE refresh_tokens SET expires_at = ? WHERE token_hash = ?",
                Instant.now().minus(1, ChronoUnit.DAYS),
                hash);

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(issued.rawToken(), "JUnit", "127.0.0.1"));

        RefreshToken row = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        assertNotNull(row.getRevokedAt());
    }

    @Test
    void revoke_isIdempotent() {
        RefreshTokenService.IssuedRefresh issued = refreshTokenService.issue(user, "JUnit", "127.0.0.1");
        refreshTokenService.revoke(issued.rawToken());

        RefreshToken afterFirst = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(issued.rawToken()))
                .orElseThrow();
        Instant revokedAt = afterFirst.getRevokedAt();
        assertNotNull(revokedAt);

        refreshTokenService.revoke(issued.rawToken());

        RefreshToken afterSecond = refreshTokenRepository
                .findByTokenHash(RefreshTokenService.hash(issued.rawToken()))
                .orElseThrow();
        assertEquals(revokedAt, afterSecond.getRevokedAt());
    }

    @Test
    void revokeAllForUser_revokesActiveTokens() {
        RefreshTokenService.IssuedRefresh one = refreshTokenService.issue(user, "JUnit", "127.0.0.1");
        RefreshTokenService.IssuedRefresh two = refreshTokenService.issue(user, "JUnit", "127.0.0.2");

        refreshTokenService.revokeAllForUser(user.getId());

        assertNotNull(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(one.rawToken()))
                .orElseThrow().getRevokedAt());
        assertNotNull(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(two.rawToken()))
                .orElseThrow().getRevokedAt());
    }
}
