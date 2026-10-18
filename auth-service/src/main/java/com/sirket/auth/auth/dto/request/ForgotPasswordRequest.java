package com.sirket.auth.auth.dto.request;

import com.sirket.auth.auth.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ForgotPasswordRequest(
		@ValidEmail String email
) {
}
