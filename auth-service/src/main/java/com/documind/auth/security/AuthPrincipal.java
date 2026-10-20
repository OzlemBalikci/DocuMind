package com.documind.auth.security;

import com.documind.auth.user.entity.Role;

public record AuthPrincipal(
        Long userId,
        Role role) {
}
