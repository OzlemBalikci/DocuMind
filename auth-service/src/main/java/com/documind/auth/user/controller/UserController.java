package com.documind.auth.user.controller;

import com.documind.auth.auth.dto.response.MeResponse;
import com.documind.auth.security.AuthPrincipal;
import com.documind.auth.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
		return userService.getMe(principal.userId());
	}
}
