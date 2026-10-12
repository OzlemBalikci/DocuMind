package com.documind.backend.auth.service;

import com.documind.backend.auth.entity.RefreshToken;
import com.documind.backend.auth.repository.RefreshTokenRepository;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.config.JwtProperties;
import com.documind.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;


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

    public record Created(RefreshToken entity, IssuedRefresh issued) {

    }

    @Transactional
    public IssuedRefresh issue(User user, String userAgent, String ipAddress) {
        return create(user, UUID.randomUUID(), userAgent, ipAddress).issued();
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefresh rotate(String rawToken, String userAgent, String ipAddress) {
        RefreshToken current = repository.findLockedByTokenHash(hash(requireToken(rawToken)))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            revokeFamily(current.getFamilyId());
            throw new InvalidRefreshTokenException();
        }
        if (!current.getExpiresAt().isAfter(clock.instant())){
            current.revoke();
            throw new InvalidRefreshTokenException();
        }

        Created next = create(current.getUser(), current.getFamilyId(), userAgent, ipAddress);
        current.revokeAsReplacedBy(next.entity());
        return new RotatedRefresh(current.getUser(), next.issued());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findLockedByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.findByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
    }

    private void revokeFamily(UUID familyId) {
        repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(RefreshToken::revoke);
    }

    private Created create(User user, UUID familyId, String userAgent, String ipAddress) {
        String raw = generateRawToken();
        Instant expiresAt = clock.instant().plus(Duration.ofDays(properties.getRefreshExpirationDays()));
        RefreshToken entity = repository.save(
                RefreshToken.create(user, familyId, hash(raw), expiresAt, userAgent, ipAddress));
        return new Created(entity, new IssuedRefresh(raw, expiresAt));
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
            throw new IllegalStateException("SHA-256 is not supported.", e);
        }
    }
}
