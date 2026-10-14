package com.documind.backend.auth.dto;

import com.documind.backend.common.validation.ValidEmail;
import com.documind.backend.common.validation.ValidFullName;
import com.documind.backend.common.validation.ValidPassword;
import com.documind.backend.common.validation.ValidRegisterRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
@ValidRegisterRequest
public record RegisterRequest(
		@ValidEmail String email,
		@ValidPassword String password,
		@ValidFullName String fullName
) {
}
