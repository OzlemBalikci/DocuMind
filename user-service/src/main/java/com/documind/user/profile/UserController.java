package com.documind.user.profile;

import com.documind.user.deletion.AccountDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMe(@AuthenticationPrincipal Jwt jwt) {
		deletions.request(jwt);
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public List<UserProfileResponse> list() {
		return profiles.listAll();
	}
}
