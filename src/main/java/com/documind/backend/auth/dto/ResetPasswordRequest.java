package com.documind.backend.auth.dto;

import com.documind.backend.common.validation.ValidEmail;
import com.documind.backend.common.validation.ValidPassword;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = false)
public record ResetPasswordRequest(
		@ValidEmail String email,
		@NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code,
		@ValidPassword String newPassword
) {
}
