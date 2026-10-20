package com.documind.auth.auth.domain;

import com.documind.auth.auth.repository.PasswordResetCodeRepository;
import com.documind.auth.common.config.PasswordResetProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PasswordResetCooldown {

	private final PasswordResetCodeRepository resetCodeRepository;
	private final PasswordResetProperties properties;
	private final Clock clock;

	public boolean isActiveForUser(Long userId) {
		Duration minInterval = properties.getMinRequestInterval();
		if (minInterval == null || minInterval.isZero() || minInterval.isNegative()) {
			return false;
		}
		Instant now = clock.instant();
		return resetCodeRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
				.map(code -> code.getCreatedAt().plus(minInterval).isAfter(now))
				.orElse(false);
	}
}
