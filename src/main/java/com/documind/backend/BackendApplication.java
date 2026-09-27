package com.documind.backend;

import com.documind.backend.config.LocalDotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		LocalDotEnvLoader.applyIfLocalProfile();
		SpringApplication.run(BackendApplication.class, args);
	}

}
