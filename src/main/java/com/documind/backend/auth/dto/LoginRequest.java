package com.documind.backend.auth.dto;

import com.documind.backend.common.validation.ValidEmail;
import com.documind.backend.common.validation.ValidationMessages;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record LoginRequest(
		@ValidEmail String email,
		@NotBlank(message = ValidationMessages.PASSWORD_REQUIRED) String password
) {
}
