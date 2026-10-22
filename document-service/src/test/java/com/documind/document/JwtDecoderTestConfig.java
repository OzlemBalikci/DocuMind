package com.documind.document;

import com.documind.common.security.TestJwtSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class JwtDecoderTestConfig {

	@Bean
	@Primary
	JwtDecoder jwtDecoder() {
		return TestJwtSupport.decoder();
	}
}
