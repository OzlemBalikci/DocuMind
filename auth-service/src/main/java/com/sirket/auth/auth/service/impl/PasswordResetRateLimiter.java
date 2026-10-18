package com.sirket.auth.auth.service.impl;

import com.documind.backend.common.exception.TooManyRequestsException;
import com.sirket.auth.common.config.PasswordResetProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class PasswordResetRateLimiter {

	private static final String FORGOT_IP = "auth:pwd-reset:forgot:ip:";
	private static final String FORGOT_USER = "auth:pwd-reset:forgot:user:";
	private static final String VERIFY_IP = "auth:pwd-reset:verify:ip:";

	private final StringRedisTemplate redisTemplate;
	private final PasswordResetProperties properties;

	public PasswordResetRateLimiter(StringRedisTemplate redisTemplate, PasswordResetProperties properties) {
		this.redisTemplate = redisTemplate;
		this.properties = properties;
	}

	public void consumeForgotIp(String ipAddress) {
		consumeOrReject(FORGOT_IP + normalize(ipAddress), properties.getForgotIpCapacity(), properties.getForgotIpWindow());
	}

	public boolean allowForgotAccount(Long userId) {
		return allow(FORGOT_USER + userId, properties.getForgotAccountCapacity(), properties.getForgotAccountWindow());
	}

	public void consumeVerifyIp(String ipAddress) {
		consumeOrReject(VERIFY_IP + normalize(ipAddress), properties.getVerifyIpCapacity(), properties.getVerifyIpWindow());
	}

	private void consumeOrReject(String key, int capacity, Duration window) {
		long count = increment(key, window);
		if (count > capacity) {
			throw new TooManyRequestsException(window);
		}
	}

	private boolean allow(String key, int capacity, Duration window) {
		try {
			return increment(key, window) <= capacity;
		} catch (RuntimeException ex) {
			log.warn("Password reset account limit could not be recorded; skipping the send");
			return false;
		}
	}

	private long increment(String key, Duration window) {
		try {
			Long count = redisTemplate.opsForValue().increment(key);
			if (count == null) {
				throw new IllegalStateException("Redis increment returned no value");
			}
			Long ttl = redisTemplate.getExpire(key);
			if (count == 1L || ttl == null || ttl < 0) {
				redisTemplate.expire(key, window);
			}
			return count;
		} catch (RuntimeException ex) {
			log.warn("Password reset rate limit could not be read");
			throw new TooManyRequestsException(window);
		}
	}

	private static String normalize(String ipAddress) {
		if (ipAddress == null || ipAddress.isBlank()) {
			return "unknown";
		}
		return ipAddress.trim();
	}
}
