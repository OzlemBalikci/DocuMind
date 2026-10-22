package com.documind.user.deletion;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clients")
public class DownstreamProperties {

	private String documentBaseUrl = "http://localhost:8082";
	private String chatBaseUrl = "http://localhost:8083";

	public String getDocumentBaseUrl() {
		return documentBaseUrl;
	}

	public void setDocumentBaseUrl(String documentBaseUrl) {
		this.documentBaseUrl = documentBaseUrl;
	}

	public String getChatBaseUrl() {
		return chatBaseUrl;
	}

	public void setChatBaseUrl(String chatBaseUrl) {
		this.chatBaseUrl = chatBaseUrl;
	}
}
