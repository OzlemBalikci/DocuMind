package com.documind.auth.auth.domain;

import com.documind.auth.user.entity.User;
import com.documind.auth.user.repository.UserRepository;
import com.documind.backend.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountDeletionPolicy {

	private final UserRepository userRepository;
	private final CredentialAuthenticator credentialAuthenticator;
	private final AccessTokenRevocation accessTokenRevocation;

	public User deleteAccount(Long userId, String password, String accessToken) {
		User user = userRepository.findById(userId)
				.orElseThrow(NotFoundException::new);
		credentialAuthenticator.requireMatchingPassword(user, password);
		accessTokenRevocation.revokeIfOwnedByUser(userId, accessToken);
		userRepository.delete(user);
		return user;
	}
}
