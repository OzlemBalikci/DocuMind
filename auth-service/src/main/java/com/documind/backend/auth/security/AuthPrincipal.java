package com.documind.backend.auth.security;

import com.documind.backend.user.entity.Role;

public record AuthPrincipal(Long userId, Role role) {
}
