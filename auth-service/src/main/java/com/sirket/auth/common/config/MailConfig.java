package com.sirket.auth.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class MailConfig {

	private final MailProperties mailProperties;

	@Bean
	public JavaMailSender javaMailSender() {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(mailProperties.getHost());
		sender.setPort(mailProperties.getPort());
		sender.setUsername(blankToNull(mailProperties.getUsername()));
		sender.setPassword(blankToNull(mailProperties.getPassword()));
		sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
		sender.setJavaMailProperties(smtpProperties());
		return sender;
	}

	private Properties smtpProperties() {
		Properties properties = new Properties();
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.auth", Boolean.toString(mailProperties.isAuth()));
		properties.put("mail.smtp.starttls.enable", Boolean.toString(mailProperties.isStarttls()));
		properties.put("mail.smtp.starttls.required", Boolean.toString(mailProperties.isStarttls()));
		properties.put("mail.smtp.ssl.enable", Boolean.toString(mailProperties.isSsl()));
		properties.put("mail.smtp.connectiontimeout", "10000");
		properties.put("mail.smtp.timeout", "10000");
		properties.put("mail.smtp.writetimeout", "10000");
		return properties;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
