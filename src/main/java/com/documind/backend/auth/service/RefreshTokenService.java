package com.documind.backend.auth.service;

import com.documind.backend.auth.entity.RefreshToken;
import com.documind.backend.auth.repository.RefreshTokenRepository;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.config.JwtProperties;
import com.documind.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    public record IssuedRefresh(String rawToken, Instant expiresAt) {
    }

    public record RotatedRefresh(User user, IssuedRefresh refresh) {
    }

    private record Created(RefreshToken entity, IssuedRefresh issued) {
    }

    @Transactional
    public IssuedRefresh issue(User user, String userAgent, String ipAddress) {
        return create(user, UUID.randomUUID(), userAgent, ipAddress).issued();
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefresh rotate(String rawToken, String userAgent, String ipAddress) {
        Instant now = clock.instant();

        RefreshToken current = repository.findLockedByTokenHash(hash(requireToken(rawToken)))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            int revoked = revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected: familyId={}, revokedTokens={}", current.getFamilyId(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (!current.isActive(now)) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }

        Created next = create(current.getUser(), current.getFamilyId(), userAgent, ipAddress);
        current.revokeAsReplacedBy(next.entity(), now);
        return new RotatedRefresh(current.getUser(), next.issued());
    }

    private Created create(User user, UUID familyId, String userAgent, String ipAddress) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        String raw = generateRawToken();
        Instant expiresAt = now.plus(Duration.ofDays(properties.getRefreshExpirationDays()))
                .truncatedTo(ChronoUnit.SECONDS);
        RefreshToken entity = repository.save(
                RefreshToken.create(user, familyId, hash(raw), expiresAt, userAgent, ipAddress, now));
        return new Created(entity, new IssuedRefresh(raw, expiresAt));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        repository.findLockedByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(now));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllActiveForUser(userId, clock.instant());
    }

    private int revokeFamily(UUID familyId, Instant now) {
        return repository.revokeFamily(familyId, now);
    }

    private static String requireToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return rawToken;
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not supported", e);
        }
    }
}
