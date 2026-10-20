package com.documind.auth.user.service.impl;

import com.documind.auth.auth.dto.response.MeResponse;
import com.documind.backend.common.exception.NotFoundException;
import com.documind.auth.user.entity.User;
import com.documind.auth.user.repository.UserRepository;
import com.documind.auth.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;

	@Override
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
}
