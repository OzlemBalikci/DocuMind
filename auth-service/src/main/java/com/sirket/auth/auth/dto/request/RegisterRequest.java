package com.sirket.auth.auth.dto.request;

import com.sirket.auth.auth.validation.ValidEmail;
import com.sirket.auth.auth.validation.ValidFullName;
import com.sirket.auth.auth.validation.ValidPassword;
import com.sirket.auth.auth.validation.ValidRegisterRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
@ValidRegisterRequest
public record RegisterRequest(
		@ValidEmail String email,
		@ValidPassword String password,
		@ValidFullName String fullName
) {
}
