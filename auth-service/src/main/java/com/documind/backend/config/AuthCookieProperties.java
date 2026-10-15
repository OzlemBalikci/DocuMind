package com.documind.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
@Getter
@Setter
public class AuthCookieProperties {

	private String name = "refresh_token";
	private String path = "/api/auth";
	private boolean secure = false;
	private String sameSite = "Lax";
}
