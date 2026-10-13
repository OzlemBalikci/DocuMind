package com.documind.backend.auth.dto;

import com.documind.backend.user.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role
) { }
