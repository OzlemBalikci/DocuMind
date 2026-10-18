package com.sirket.auth.auth.controller;

import com.sirket.auth.auth.dto.response.AuthResponse;
import com.sirket.auth.auth.dto.request.DeleteAccountRequest;
import com.sirket.auth.auth.dto.request.ForgotPasswordRequest;
import com.sirket.auth.auth.dto.response.IssuedAuthSession;
import com.sirket.auth.auth.dto.request.LoginRequest;
import com.sirket.auth.auth.dto.request.LogoutRequest;
import com.sirket.auth.auth.dto.request.RefreshRequest;
import com.sirket.auth.auth.dto.request.RegisterRequest;
import com.sirket.auth.auth.dto.request.ResetPasswordRequest;
import com.sirket.auth.security.AuthPrincipal;
import com.sirket.auth.security.RefreshTokenCookieSupport;
import com.sirket.auth.auth.service.AuthService;
import com.sirket.auth.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final PasswordResetService passwordResetService;
	private final RefreshTokenCookieSupport refreshTokenCookieSupport;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(
			@Valid @RequestBody RegisterRequest body,
			HttpServletRequest httpRequest) {
		IssuedAuthSession session = authService.register(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.CREATED, session);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@Valid @RequestBody LoginRequest body,
			HttpServletRequest httpRequest) {
		IssuedAuthSession session = authService.login(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.OK, session);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
			@RequestBody(required = false) RefreshRequest body,
			HttpServletRequest httpRequest) {
		String refreshToken = firstNonBlank(
				refreshTokenCookieSupport.readRefresh(httpRequest),
				body != null ? body.refreshToken() : null);
		IssuedAuthSession session = authService.refresh(
				refreshToken,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return withRefreshCookie(HttpStatus.OK, session);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@Valid @RequestBody LogoutRequest body,
			HttpServletRequest httpRequest) {
		String refreshToken = firstNonBlank(
				refreshTokenCookieSupport.readRefresh(httpRequest),
				body.refreshToken());
		authService.logout(refreshToken, body.accessToken());
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieSupport.clearCookie().toString())
				.build();
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Void> forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest body,
			HttpServletRequest httpRequest) {
		passwordResetService.forgotPassword(body, clientIp(httpRequest));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Void> resetPassword(
			@Valid @RequestBody ResetPasswordRequest body,
			HttpServletRequest httpRequest) {
		passwordResetService.resetPassword(body, clientIp(httpRequest));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/account")
	public ResponseEntity<Void> deleteAccount(
			@AuthenticationPrincipal AuthPrincipal principal,
			@Valid @RequestBody DeleteAccountRequest body,
			HttpServletRequest httpRequest) {
		authService.deleteAccount(principal.userId(), body.password(), bearerToken(httpRequest));
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, refreshTokenCookieSupport.clearCookie().toString())
				.build();
	}

	private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, IssuedAuthSession session) {
		ResponseCookie cookie = refreshTokenCookieSupport.toSetCookie(
				session.refreshToken(),
				session.refreshExpiresAt());
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(session.response());
	}

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}

	private static String bearerToken(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring("Bearer ".length()).trim();
		}
		return null;
	}

	private static String userAgent(HttpServletRequest request) {
		return request.getHeader(HttpHeaders.USER_AGENT);
	}

	private static String clientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
