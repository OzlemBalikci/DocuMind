package com.documind.auth.token.service.impl;

import com.documind.auth.token.service.AccessTokenBlacklistService;
import com.documind.common.security.AccessTokenBlacklistKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccessTokenBlacklistServiceImpl implements AccessTokenBlacklistService {

	private final StringRedisTemplate redisTemplate;
	private final Clock clock;

	@Override
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

	@Override
	public boolean isRevoked(String jti) {
		if (jti == null || jti.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(redisTemplate.hasKey(AccessTokenBlacklistKeys.PREFIX + jti));
	}
}
