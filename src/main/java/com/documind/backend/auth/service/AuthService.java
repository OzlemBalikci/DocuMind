package com.documind.backend.auth.service;

import com.documind.backend.auth.dto.AuthResponse;
import com.documind.backend.auth.dto.LoginRequest;
import com.documind.backend.auth.dto.LogoutRequest;
import com.documind.backend.auth.dto.MeResponse;
import com.documind.backend.auth.dto.RefreshRequest;
import com.documind.backend.auth.dto.RegisterRequest;
import com.documind.backend.auth.dto.UserResponse;
import com.documind.backend.common.exception.EmailAlreadyExistsException;
import com.documind.backend.common.exception.InvalidCredentialsException;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.common.exception.NotFoundException;
import com.documind.backend.user.entity.User;
import com.documind.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public AuthResponse register(RegisterRequest request, String userAgent, String ipAddress) {
		if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
			throw new EmailAlreadyExistsException();
		}

		User user = User.builder()
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.fullName(request.fullName().trim())
				.build();

		try {
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyExistsException();
		}

		return issueTokens(user, userAgent, ipAddress);
	}

	@Transactional
	public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
		User user = userRepository.findByEmailIgnoreCase(request.email().trim())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(InvalidCredentialsException::new);

		return issueTokens(user, userAgent, ipAddress);
	}

	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public AuthResponse refresh(RefreshRequest request, String userAgent, String ipAddress) {
		RefreshTokenService.RotatedRefresh rotated =
				refreshTokenService.rotate(request.refreshToken(), userAgent, ipAddress);

		JwtService.AccessToken access = jwtService.issueAccess(rotated.user().getId());
		return toAuthResponse(rotated.user(), access, rotated.refresh());
	}

	public void logout(LogoutRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}

	@Transactional(readOnly = true)
	public MeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(NotFoundException::new);

		return new MeResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole(),
				null);
	}

	private AuthResponse issueTokens(User user, String userAgent, String ipAddress) {
		JwtService.AccessToken access = jwtService.issueAccess(user.getId());
		RefreshTokenService.IssuedRefresh refresh = refreshTokenService.issue(user, userAgent, ipAddress);
		return toAuthResponse(user, access, refresh);
	}

	private AuthResponse toAuthResponse(User user,
			JwtService.AccessToken access,
			RefreshTokenService.IssuedRefresh refresh) {
		return new AuthResponse(
				AuthResponse.TOKEN_TYPE_BEARER,
				access.token(),
				access.expiresAt(),
				access.expiresInSeconds(),
				refresh.rawToken(),
				refresh.expiresAt(),
				toUserResponse(user));
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole());
	}
}
