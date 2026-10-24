package com.documind.user.service;

import com.documind.user.dto.response.AdminProfileResponse;
import com.documind.user.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface ProfileService {

	UserProfileResponse getOrCreate(Jwt jwt);

	Page<AdminProfileResponse> listAll(Pageable pageable);
}
