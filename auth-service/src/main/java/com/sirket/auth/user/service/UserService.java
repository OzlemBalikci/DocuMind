package com.sirket.auth.user.service;

import com.sirket.auth.auth.dto.response.MeResponse;

public interface UserService {

	MeResponse getMe(Long userId);
}
