package com.sirket.auth.auth.service.impl;

import com.sirket.auth.auth.dto.request.LoginRequest;
import com.sirket.auth.auth.dto.request.RegisterRequest;
import com.sirket.auth.auth.dto.response.AuthResponse;
import com.sirket.auth.auth.dto.response.IssuedAuthSession;
import com.sirket.auth.auth.dto.response.UserResponse;
import com.sirket.auth.auth.service.AuthService;
import com.sirket.auth.common.audit.AuditService;
import com.sirket.auth.common.util.Texts;
import com.sirket.auth.token.service.AccessTokenBlacklistService;
import com.sirket.auth.token.service.RefreshTokenService;
import com.sirket.auth.token.service.TokenService;
import com.sirket.auth.user.entity.User;
import com.sirket.auth.user.repository.UserRepository;
import com.documind.backend.common.exception.EmailAlreadyExistsException;
import com.documind.backend.common.exception.InvalidCredentialsException;
import com.documind.backend.common.exception.InvalidRefreshTokenException;
import com.documind.backend.common.exception.NotFoundException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokenService;
	private final AccessTokenBlacklistService accessTokenBlackListService;
	private final AuditService auditService;

	@Override
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

		auditService.authEvent("register", user.getId(), ipAddress);
		return issueTokens(user, userAgent, ipAddress);
	}

	@Override
	@Transactional
	public IssuedAuthSession login(LoginRequest request, String userAgent, String ipAddress) {
		String email = Texts.normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(InvalidCredentialsException::new);

		auditService.authEvent("login", user.getId(), ipAddress);
		return issueTokens(user, userAgent, ipAddress);
	}

	@Override
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public IssuedAuthSession refresh(String refreshToken, String userAgent, String ipAddress) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		RefreshTokenService.RotatedRefresh rotated =
				refreshTokenService.rotate(refreshToken, userAgent, ipAddress);

		TokenService.AccessToken access = tokenService.issueAccess(rotated.user().getId());
		auditService.authEvent("refresh", rotated.user().getId(), ipAddress);
		return toIssuedSession(rotated.user(), access, rotated.refresh());
	}

	@Override
	public void logout(String refreshToken, String accessToken) {
		refreshTokenService.revoke(refreshToken);
		try {
			TokenService.AccessClaims claims = tokenService.parseAccess(accessToken);
			accessTokenBlackListService.revokeUntilExpiry(claims.jti(), claims.expiresAt());
		} catch (JwtException ignored) {
			// Expired or malformed access token should not fail logout.
		}
	}

	@Override
	@Transactional
	public void deleteAccount(Long userId, String password, String accessToken) {
		User user = userRepository.findById(userId)
				.orElseThrow(NotFoundException::new);
		if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		if (accessToken != null && !accessToken.isBlank()) {
			try {
				TokenService.AccessClaims claims = tokenService.parseAccess(accessToken);
				if (userId.equals(claims.userId())) {
					accessTokenBlackListService.revokeUntilExpiry(claims.jti(), claims.expiresAt());
				}
			} catch (JwtException ignored) {
				// Account deletion still proceeds when the access token cannot be parsed.
			}
		}
		userRepository.delete(user);
		auditService.authEvent("delete_account", userId, null);
	}

	private IssuedAuthSession issueTokens(User user, String userAgent, String ipAddress) {
		TokenService.AccessToken access = tokenService.issueAccess(user.getId());
		RefreshTokenService.IssuedRefresh refresh = refreshTokenService.issue(user, userAgent, ipAddress);
		return toIssuedSession(user, access, refresh);
	}

	private IssuedAuthSession toIssuedSession(User user,
			TokenService.AccessToken access,
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
