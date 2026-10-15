package com.documind.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.password-reset")
@Getter
@Setter
public class PasswordResetProperties {

	private Duration codeTtl = Duration.ofMinutes(15);
	private Duration minRequestInterval = Duration.ofMinutes(1);
	private String mailSubject = "DocuMind password reset code";
}
