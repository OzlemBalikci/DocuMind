package com.documind.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AccessTokenVerifierConfiguration {

	@Bean
	AccessTokenVerifier accessTokenVerifier(@Value("${app.jwt.secret}") String secret) {
		return new AccessTokenVerifier(secret, Clock.systemUTC());
	}
}
