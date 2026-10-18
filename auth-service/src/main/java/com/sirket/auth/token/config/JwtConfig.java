package com.sirket.auth.token.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class JwtConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
