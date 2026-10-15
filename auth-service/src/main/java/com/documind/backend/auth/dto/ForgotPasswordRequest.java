package com.documind.backend.auth.dto;

import com.documind.backend.common.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ForgotPasswordRequest(
		@ValidEmail String email
) {
}
