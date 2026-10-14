package com.documind.backend.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.llm")
@Validated
public class LlmProperties {

	@Positive
	private int dailyLimitPerUser = 50;

	public int getDailyLimitPerUser() {
		return dailyLimitPerUser;
	}

	public void setDailyLimitPerUser(int dailyLimitPerUser) {
		this.dailyLimitPerUser = dailyLimitPerUser;
	}
}
