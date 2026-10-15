package com.documind.backend.auth.service;

import com.documind.common.security.AccessTokenBlacklistKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public void revokeUntilExpiry(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (!ttl.isPositive()) {
            return;
        }
        redisTemplate.opsForValue().set(AccessTokenBlacklistKeys.PREFIX + jti, "1", ttl);
    }

    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(AccessTokenBlacklistKeys.PREFIX + jti));
    }
}
