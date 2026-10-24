package com.documind.user.service;

import org.springframework.security.oauth2.jwt.Jwt;

public interface AccountDeletionService {

	void request(Jwt jwt);
}
