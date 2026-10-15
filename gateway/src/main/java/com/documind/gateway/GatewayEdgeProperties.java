package com.documind.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.gateway")
public class GatewayEdgeProperties {

	private String authUri = "http://localhost:8081";
	private String documentUri = "http://localhost:8082";
	private String chatUri = "http://localhost:8083";
	private DataSize maxRequestSize = DataSize.ofMegabytes(2);
	private int loginCapacity = 5;
	private Duration loginPeriod = Duration.ofMinutes(1);
	private int chatCapacity = 30;
	private Duration chatPeriod = Duration.ofMinutes(1);

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

	public int getLoginCapacity() {
		return loginCapacity;
	}

	public void setLoginCapacity(int loginCapacity) {
		this.loginCapacity = loginCapacity;
	}

	public Duration getLoginPeriod() {
		return loginPeriod;
	}

	public void setLoginPeriod(Duration loginPeriod) {
		this.loginPeriod = loginPeriod;
	}

	public int getChatCapacity() {
		return chatCapacity;
	}

	public void setChatCapacity(int chatCapacity) {
		this.chatCapacity = chatCapacity;
	}

	public Duration getChatPeriod() {
		return chatPeriod;
	}

	public void setChatPeriod(Duration chatPeriod) {
		this.chatPeriod = chatPeriod;
	}
}
