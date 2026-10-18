package com.sirket.auth.auth.domain;

import com.sirket.auth.auth.dto.request.ResetPasswordRequest;
import com.sirket.auth.auth.entity.PasswordResetCode;
import com.sirket.auth.auth.repository.PasswordResetCodeRepository;
import com.sirket.auth.token.repository.RefreshTokenRepository;
import com.sirket.auth.user.entity.User;
import com.sirket.auth.user.repository.UserRepository;
import com.sirket.auth.common.config.PasswordResetProperties;
import com.sirket.auth.common.util.Texts;
import com.documind.backend.common.exception.InvalidPasswordResetCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordResetCompletion {

	private final UserRepository userRepository;
	private final PasswordResetCodeRepository resetCodeRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final ResetCodeHasher codeHasher;
	private final PasswordResetProperties properties;
	private final Clock clock;

	public void complete(ResetPasswordRequest request) {
		User user = userRepository.findByEmail(Texts.normalizeEmail(request.email()))
				.orElseThrow(InvalidPasswordResetCodeException::new);

		Instant now = clock.instant();
		resetCodeRepository.invalidateExpiredForUser(user.getId(), now);

		List<PasswordResetCode> active = resetCodeRepository.findActiveForUserLocked(user.getId(), now);
		if (active.isEmpty()) {
			throw new InvalidPasswordResetCodeException();
		}

		PasswordResetCode resetCode = active.getFirst();
		int maxAttempts = properties.getMaxFailedAttempts();
		if (resetCode.isLockedOut(maxAttempts)) {
			resetCode.markUsed(now);
			throw new InvalidPasswordResetCodeException();
		}

		if (!resetCode.matchesPlainCode(request.code(), codeHasher)) {
			resetCode.recordWrongAttempt(maxAttempts, now);
			throw new InvalidPasswordResetCodeException();
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		resetCode.markUsed(now);
		resetCodeRepository.invalidateActiveForUser(user.getId(), now);
		refreshTokenRepository.revokeAllActiveForUser(user.getId(), now);
	}
}
