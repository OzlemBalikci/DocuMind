package com.documind.user.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.clients")
public class DownstreamProperties {

	private String documentBaseUrl = "http://localhost:8082";
	private String chatBaseUrl = "http://localhost:8083";
}
