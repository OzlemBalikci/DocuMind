package com.documind.backend.auth.controller;

import com.documind.backend.auth.dto.AuthResponse;
import com.documind.backend.auth.dto.LoginRequest;
import com.documind.backend.auth.dto.LogoutRequest;
import com.documind.backend.auth.dto.MeResponse;
import com.documind.backend.auth.dto.RefreshRequest;
import com.documind.backend.auth.dto.RegisterRequest;
import com.documind.backend.auth.security.AuthPrincipal;
import com.documind.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(
			@Valid @RequestBody RegisterRequest body,
			HttpServletRequest httpRequest) {
		AuthResponse response = authService.register(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@Valid @RequestBody LoginRequest body,
			HttpServletRequest httpRequest) {
		AuthResponse response = authService.login(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(
			@Valid @RequestBody RefreshRequest body,
			HttpServletRequest httpRequest) {
		AuthResponse response = authService.refresh(
				body,
				userAgent(httpRequest),
				clientIp(httpRequest));
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest body) {
		authService.logout(body);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
		return authService.getMe(principal.userId());
	}

	private static String userAgent(HttpServletRequest request) {
		return request.getHeader(HttpHeaders.USER_AGENT);
	}

	private static String clientIp(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
