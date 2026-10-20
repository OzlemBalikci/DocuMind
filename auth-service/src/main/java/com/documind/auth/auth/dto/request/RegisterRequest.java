package com.documind.auth.auth.dto.request;

import com.documind.auth.auth.validation.ValidEmail;
import com.documind.auth.auth.validation.ValidFullName;
import com.documind.auth.auth.validation.ValidPassword;
import com.documind.auth.auth.validation.ValidRegisterRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
@ValidRegisterRequest
public record RegisterRequest(
		@ValidEmail String email,
		@ValidPassword String password,
		@ValidFullName String fullName
) {
}
