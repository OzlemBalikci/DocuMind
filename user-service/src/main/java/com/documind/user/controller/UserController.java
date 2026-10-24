package com.documind.user.controller;

import com.documind.user.dto.response.AdminProfileResponse;
import com.documind.user.dto.response.UserProfileResponse;
import com.documind.user.service.AccountDeletionService;
import com.documind.user.service.ProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final ProfileService profiles;
	private final AccountDeletionService deletions;

	public UserController(ProfileService profiles, AccountDeletionService deletions) {
		this.profiles = profiles;
		this.deletions = deletions;
	}

	@GetMapping("/me")
	public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
		return profiles.getOrCreate(jwt);
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void deleteMe(@AuthenticationPrincipal Jwt jwt) {
		deletions.request(jwt);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Page<AdminProfileResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		return profiles.listAll(pageable);
	}
}
