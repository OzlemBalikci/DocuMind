package com.sirket.auth.auth.dto.response;

import com.sirket.auth.user.entity.Role;

public record MeResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        QuotaResponse quota
) { }
