package com.sirket.auth.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

	private String host = "";
	private int port = 587;
	private String username = "";
	private String password = "";
	private boolean auth = true;
	private boolean starttls = true;
	private boolean ssl = false;
	private String from = "";
}
