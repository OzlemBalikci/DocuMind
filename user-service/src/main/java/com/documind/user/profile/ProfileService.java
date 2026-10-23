package com.documind.user.profile;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

public interface ProfileService {

	UserProfileResponse getOrCreate(Jwt jwt);

	Page<AdminProfileResponse> listAll(Pageable pageable);
}
