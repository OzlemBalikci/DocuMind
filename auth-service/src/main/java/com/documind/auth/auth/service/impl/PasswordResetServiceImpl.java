package com.documind.auth.auth.service.impl;

import com.documind.auth.auth.domain.PasswordResetCompletion;
import com.documind.auth.auth.domain.PasswordResetIssuer;
import com.documind.auth.auth.dto.request.ForgotPasswordRequest;
import com.documind.auth.auth.dto.request.ResetPasswordRequest;
import com.documind.auth.auth.service.PasswordResetService;
import com.documind.auth.user.repository.UserRepository;
import com.documind.auth.common.util.Texts;
import com.documind.backend.common.exception.InvalidPasswordResetCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

	private final UserRepository userRepository;
	private final PasswordResetRateLimiter rateLimiter;
	private final PasswordResetIssuer passwordResetIssuer;
	private final PasswordResetCompletion passwordResetCompletion;

	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
		rateLimiter.consumeForgotIp(ipAddress);
		String email = Texts.normalizeEmail(request.email());
		userRepository.findByEmail(email).ifPresent(passwordResetIssuer::issueForUserIfAllowed);
	}

	@Override
	@Transactional(noRollbackFor = InvalidPasswordResetCodeException.class)
	public void resetPassword(ResetPasswordRequest request, String ipAddress) {
		rateLimiter.consumeVerifyIp(ipAddress);
		passwordResetCompletion.complete(request);
	}
}
