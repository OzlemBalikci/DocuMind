package com.documind.auth.auth.domain;

import com.documind.auth.auth.dto.request.RegisterRequest;
import com.documind.auth.user.entity.User;
import com.documind.auth.user.repository.UserRepository;
import com.documind.auth.common.util.Texts;
import com.documind.backend.common.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationPolicy {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public User register(RegisterRequest request) {
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
			return userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyExistsException();
		}
	}
}
