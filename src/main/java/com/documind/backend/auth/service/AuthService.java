package com.documind.backend.auth.service;

import com.documind.backend.auth.dto.AuthResponse;
import com.documind.backend.auth.dto.IssuedAuthSession;
import com.documind.backend.auth.dto.LoginRequest;
import com.documind.backend.auth.dto.MeResponse;
import com.documind.backend.auth.dto.RegisterRequest;
import com.documind.backend.auth.dto.UserResponse;
import com.documind.backend.common.exception.EmailAlreadyExistsException;
import com.documind.backend.common.util.Texts;
import com.documind.backend.common.exception.InvalidCredentialsException;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.common.exception.NotFoundException;
import com.documind.backend.user.entity.User;
import com.documind.backend.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
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
	private final AccessTokenBlacklistService accessTokenBlackListService;

	@Transactional
	public IssuedAuthSession register(RegisterRequest request, String userAgent, String ipAddress) {
		String email = Texts.normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}

		User user = User.builder()
				.email(email)
				.passwordHash(passwordEncoder.encode(request.password()))
				.fullName(Texts.normalizeFullName(request.fullName()))
				.build();

		try {
			user = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyExistsException();
		}

		return issueTokens(user, userAgent, ipAddress);
	}

	@Transactional
	public IssuedAuthSession login(LoginRequest request, String userAgent, String ipAddress) {
		String email = Texts.normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(InvalidCredentialsException::new);

		return issueTokens(user, userAgent, ipAddress);
	}

	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public IssuedAuthSession refresh(String refreshToken, String userAgent, String ipAddress) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		RefreshTokenService.RotatedRefresh rotated =
				refreshTokenService.rotate(refreshToken, userAgent, ipAddress);

		JwtService.AccessToken access = jwtService.issueAccess(rotated.user().getId());
		return toIssuedSession(rotated.user(), access, rotated.refresh());
	}

	public void logout(String refreshToken, String accessToken) {
		refreshTokenService.revoke(refreshToken);
		try {
			JwtService.AccessClaims claims = jwtService.parseAccess(accessToken);
			accessTokenBlackListService.revokeUntilExpiry(claims.jti(), claims.expiresAt());
		} catch (JwtException ignored) {
			// Expired or malformed access token should not fail logout.
		}
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

	private IssuedAuthSession issueTokens(User user, String userAgent, String ipAddress) {
		JwtService.AccessToken access = jwtService.issueAccess(user.getId());
		RefreshTokenService.IssuedRefresh refresh = refreshTokenService.issue(user, userAgent, ipAddress);
		return toIssuedSession(user, access, refresh);
	}

	private IssuedAuthSession toIssuedSession(User user,
			JwtService.AccessToken access,
			RefreshTokenService.IssuedRefresh refresh) {
		AuthResponse response = new AuthResponse(
				AuthResponse.TOKEN_TYPE_BEARER,
				access.token(),
				access.expiresAt(),
				access.expiresInSeconds(),
				toUserResponse(user));
		return new IssuedAuthSession(response, refresh.rawToken(), refresh.expiresAt());
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole());
	}
}
