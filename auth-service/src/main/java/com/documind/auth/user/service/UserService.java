package com.documind.auth.user.service;

import com.documind.auth.auth.dto.response.MeResponse;

public interface UserService {

	MeResponse getMe(Long userId);
}
