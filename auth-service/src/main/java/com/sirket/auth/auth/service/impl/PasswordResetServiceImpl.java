package com.sirket.auth.auth.service.impl;

import com.sirket.auth.auth.dto.request.ForgotPasswordRequest;
import com.sirket.auth.auth.dto.request.ResetPasswordRequest;
import com.sirket.auth.auth.service.PasswordResetService;
import com.sirket.auth.auth.entity.PasswordResetCode;
import com.sirket.auth.user.entity.User;
import com.sirket.auth.auth.repository.PasswordResetCodeRepository;
import com.sirket.auth.token.repository.RefreshTokenRepository;
import com.sirket.auth.user.repository.UserRepository;
import com.documind.backend.common.exception.InvalidPasswordResetCodeException;
import com.sirket.auth.common.util.Texts;
import com.sirket.auth.common.config.PasswordResetProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordResetCodeRepository resetCodeRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthEmailService authEmailService;
	private final PasswordResetProperties properties;
	private final PasswordResetCodeHasher codeHasher;
	private final PasswordResetRateLimiter rateLimiter;
	private final Clock clock;

	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
		rateLimiter.consumeForgotIp(ipAddress);
		String email = Texts.normalizeEmail(request.email());
		userRepository.findByEmail(email).ifPresent(this::issueIfAllowed);
	}

	@Override
	@Transactional(noRollbackFor = InvalidPasswordResetCodeException.class)
	public void resetPassword(ResetPasswordRequest request, String ipAddress) {
		rateLimiter.consumeVerifyIp(ipAddress);
		User user = userRepository.findByEmail(Texts.normalizeEmail(request.email()))
				.orElseThrow(InvalidPasswordResetCodeException::new);
		Instant now = clock.instant();
		resetCodeRepository.invalidateExpiredForUser(user.getId(), now);

		List<PasswordResetCode> active = resetCodeRepository.findActiveForUserLocked(user.getId(), now);
		if (active.isEmpty()) {
			throw new InvalidPasswordResetCodeException();
		}
		PasswordResetCode resetCode = active.getFirst();
		if (resetCode.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
			resetCode.markUsed(now);
			throw new InvalidPasswordResetCodeException();
		}
		if (!matches(resetCode, request.code())) {
			resetCode.recordFailedAttempt();
			if (resetCode.getFailedAttempts() >= properties.getMaxFailedAttempts()) {
				resetCode.markUsed(now);
			}
			throw new InvalidPasswordResetCodeException();
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		resetCode.markUsed(now);
		resetCodeRepository.invalidateActiveForUser(user.getId(), now);
		refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
	}

	private void issueIfAllowed(User user) {
		if (!rateLimiter.allowForgotAccount(user.getId())) {
			log.debug("Password reset skipped for user id {} because of the account limit", user.getId());
			return;
		}
		if (isWithinCooldown(user.getId())) {
			log.debug("Password reset skipped for user id {} because of cooldown", user.getId());
			return;
		}
		String code = generateNumericCode();
		Instant now = clock.instant();
		resetCodeRepository.invalidateActiveForUser(user.getId(), now);
		resetCodeRepository.save(PasswordResetCode.issue(
				user,
				codeHasher.hmac(code),
				now.plus(properties.getCodeTtl())));
		try {
			authEmailService.sendPasswordResetCode(user.getEmail(), code, properties.getCodeTtl());
		} catch (RuntimeException ex) {
			log.error("Password reset email could not be sent for user id {}", user.getId(), ex);
		}
	}

	private boolean matches(PasswordResetCode resetCode, String presentedCode) {
		String presented = codeHasher.hmac(presentedCode.trim());
		return MessageDigest.isEqual(
				presented.getBytes(StandardCharsets.UTF_8),
				resetCode.getCodeHash().getBytes(StandardCharsets.UTF_8));
	}

	private boolean isWithinCooldown(Long userId) {
		Duration minInterval = properties.getMinRequestInterval();
		if (minInterval == null || minInterval.isZero() || minInterval.isNegative()) {
			return false;
		}
		Instant now = clock.instant();
		return resetCodeRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
				.map(PasswordResetCode::getCreatedAt)
				.filter(created -> created.plus(minInterval).isAfter(now))
				.isPresent();
	}

	private static String generateNumericCode() {
		return String.format("%06d", RANDOM.nextInt(1_000_000));
	}
}
