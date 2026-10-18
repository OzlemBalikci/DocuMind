package com.sirket.auth.auth.domain;

import com.sirket.auth.user.entity.User;
import com.sirket.auth.user.repository.UserRepository;
import com.sirket.auth.common.util.Texts;
import com.documind.backend.common.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialAuthenticator {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public User authenticate(String email, String rawPassword) {
		String normalizedEmail = Texts.normalizeEmail(email);
		return userRepository.findByEmail(normalizedEmail)
				.filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
				.orElseThrow(InvalidCredentialsException::new);
	}

	public void requireMatchingPassword(User user, String rawPassword) {
		if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
	}
}
