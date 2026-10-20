package com.documind.auth;

import com.documind.auth.common.config.LocalDotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sirket.auth", "com.documind.backend.common"})
public class AuthServiceApplication {

	public static void main(String[] args) {
		LocalDotEnvLoader.applyIfLocalProfile();
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
