package com.documind.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Logout writes {@code auth:access-blacklist:{jti}} from auth-service.
 * Document and chat read the same keys and reject the token before it expires.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklist {

	private final StringRedisTemplate redisTemplate;

	public boolean isRevoked(String jti) {
		if (jti == null || jti.isBlank()) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(AccessTokenBlacklistKeys.PREFIX + jti));
		} catch (RuntimeException ex) {
			log.warn("Access-token blacklist could not be read; rejecting the token");
			return true;
		}
	}
}
