package com.documind.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.gateway")
public class GatewayEdgeProperties {

	private String authUri = "http://localhost:8081";
	private String documentUri = "http://localhost:8082";
	private String chatUri = "http://localhost:8083";
	private DataSize maxRequestSize = DataSize.ofMegabytes(2);

	public String getAuthUri() {
		return authUri;
	}

	public void setAuthUri(String authUri) {
		this.authUri = authUri;
	}

	public String getDocumentUri() {
		return documentUri;
	}

	public void setDocumentUri(String documentUri) {
		this.documentUri = documentUri;
	}

	public String getChatUri() {
		return chatUri;
	}

	public void setChatUri(String chatUri) {
		this.chatUri = chatUri;
	}

	public DataSize getMaxRequestSize() {
		return maxRequestSize;
	}

	public void setMaxRequestSize(DataSize maxRequestSize) {
		this.maxRequestSize = maxRequestSize;
	}
}
