package com.documind.auth.auth.dto.response;

import com.documind.auth.user.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role
) { }
