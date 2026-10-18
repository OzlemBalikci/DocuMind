package com.documind.auth.auth.dto.request;

import com.documind.auth.auth.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ForgotPasswordRequest(
		@ValidEmail String email
) {
}
