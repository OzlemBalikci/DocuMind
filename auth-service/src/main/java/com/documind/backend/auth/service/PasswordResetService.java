package com.documind.backend.auth.service;

import com.documind.backend.auth.dto.ForgotPasswordRequest;
import com.documind.backend.auth.dto.ResetPasswordRequest;
import com.documind.backend.auth.entity.PasswordResetCode;
import com.documind.backend.auth.repository.PasswordResetCodeRepository;
import com.documind.backend.auth.repository.RefreshTokenRepository;
import com.documind.backend.common.exception.InvalidPasswordResetCodeException;
import com.documind.backend.common.util.Texts;
import com.documind.backend.config.PasswordResetProperties;
import com.documind.backend.user.entity.User;
import com.documind.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final PasswordResetCodeRepository resetCodeRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthEmailService authEmailService;
	private final PasswordResetProperties properties;
	private final Clock clock;

	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		String email = Texts.normalizeEmail(request.email());
		userRepository.findByEmail(email).ifPresent(user -> {
			if (isWithinCooldown(user.getId())) {
				log.debug("Password reset skipped for user id {} because of cooldown", user.getId());
				return;
			}
			String code = generateNumericCode();
			Instant now = clock.instant();
			resetCodeRepository.invalidateActiveForUser(user.getId(), now);
			resetCodeRepository.save(PasswordResetCode.issue(
					user,
					RefreshTokenService.hash(code),
					now.plus(properties.getCodeTtl())));
			try {
				authEmailService.sendPasswordResetCode(user.getEmail(), code, properties.getCodeTtl());
			} catch (RuntimeException ex) {
				log.error("Password reset email could not be sent for user id {}", user.getId(), ex);
			}
		});
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		User user = userRepository.findByEmail(Texts.normalizeEmail(request.email()))
				.orElseThrow(InvalidPasswordResetCodeException::new);
		Instant now = clock.instant();
		PasswordResetCode resetCode = resetCodeRepository
				.findValidForUser(user.getId(), RefreshTokenService.hash(request.code().trim()), now)
				.orElseThrow(InvalidPasswordResetCodeException::new);

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		resetCode.markUsed(now);
		resetCodeRepository.invalidateActiveForUser(user.getId(), now);
		refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
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
