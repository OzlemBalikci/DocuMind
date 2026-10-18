package com.sirket.auth.auth.dto.request;

import com.documind.backend.common.validation.ValidationMessages;

import com.sirket.auth.auth.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record LoginRequest(
		@ValidEmail String email,
		@NotBlank(message = ValidationMessages.PASSWORD_REQUIRED) String password
) {
}
