package com.sirket.auth.common.config;

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
	private String pepper = "";
	private int forgotIpCapacity = 5;
	private Duration forgotIpWindow = Duration.ofMinutes(15);
	private int forgotAccountCapacity = 3;
	private Duration forgotAccountWindow = Duration.ofHours(1);
	private int verifyIpCapacity = 10;
	private Duration verifyIpWindow = Duration.ofMinutes(15);
	private int maxFailedAttempts = 5;
}
