package com.documind.auth.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = false)
public record DeleteAccountRequest(
		@NotBlank String password
) {
}
