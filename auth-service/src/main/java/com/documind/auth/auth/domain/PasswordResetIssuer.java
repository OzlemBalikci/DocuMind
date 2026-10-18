package com.documind.auth.auth.domain;

import com.documind.auth.auth.entity.PasswordResetCode;
import com.documind.auth.auth.repository.PasswordResetCodeRepository;
import com.documind.auth.auth.service.impl.AuthEmailService;
import com.documind.auth.auth.service.impl.PasswordResetRateLimiter;
import com.documind.auth.user.entity.User;
import com.documind.auth.common.config.PasswordResetProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetIssuer {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final PasswordResetCodeRepository resetCodeRepository;
	private final PasswordResetRateLimiter rateLimiter;
	private final PasswordResetProperties properties;
	private final ResetCodeHasher codeHasher;
	private final PasswordResetCooldown cooldown;
	private final AuthEmailService authEmailService;
	private final Clock clock;

	public void issueForUserIfAllowed(User user) {
		if (!rateLimiter.allowForgotAccount(user.getId())) {
			log.debug("Password reset skipped for user id {} because of the account limit", user.getId());
			return;
		}
		if (cooldown.isActiveForUser(user.getId())) {
			log.debug("Password reset skipped for user id {} because of cooldown", user.getId());
			return;
		}

		String code = generateNumericCode();
		Instant now = clock.instant();
		resetCodeRepository.invalidateActiveForUser(user.getId(), now);
		resetCodeRepository.save(PasswordResetCode.issue(
				user,
				codeHasher.hash(code),
				now.plus(properties.getCodeTtl())));

		try {
			authEmailService.sendPasswordResetCode(user.getEmail(), code, properties.getCodeTtl());
		} catch (RuntimeException ex) {
			log.error("Password reset email could not be sent for user id {}", user.getId(), ex);
		}
	}

	private static String generateNumericCode() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}
}
