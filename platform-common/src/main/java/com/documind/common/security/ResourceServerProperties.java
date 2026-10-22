package com.documind.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class ResourceServerProperties {

	/**
	 * Access-token audience that resource servers accept ({@code aud} claim).
	 */
	private String audience = "documind-api";

	/**
	 * When true, {@code /internal/**} requires {@code ROLE_SERVICE}.
	 * Gateway sets this to false; it does not proxy internal paths.
	 */
	private boolean protectInternal = true;

	public String getAudience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = audience;
	}

	public boolean isProtectInternal() {
		return protectInternal;
	}

	public void setProtectInternal(boolean protectInternal) {
		this.protectInternal = protectInternal;
	}
}
