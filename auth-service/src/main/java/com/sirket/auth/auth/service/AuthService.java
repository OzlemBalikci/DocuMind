package com.sirket.auth.auth.service;

import com.sirket.auth.auth.dto.request.LoginRequest;
import com.sirket.auth.auth.dto.request.RegisterRequest;
import com.sirket.auth.auth.dto.response.IssuedAuthSession;

public interface AuthService {

	IssuedAuthSession register(RegisterRequest request, String userAgent, String ipAddress);

	IssuedAuthSession login(LoginRequest request, String userAgent, String ipAddress);

	IssuedAuthSession refresh(String refreshToken, String userAgent, String ipAddress);

	void logout(String refreshToken, String accessToken);

	void deleteAccount(Long userId, String password, String accessToken);
}
