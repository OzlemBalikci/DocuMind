package com.sirket.auth.security;

import com.sirket.auth.user.entity.Role;

public record AuthPrincipal(
        Long userId,
        Role role) {
}
