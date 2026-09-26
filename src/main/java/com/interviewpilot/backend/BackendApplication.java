package com.interviewpilot.backend;

import com.interviewpilot.backend.config.LocalDotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		LocalDotEnvLoader.applyIfLocalProfile();
		SpringApplication.run(BackendApplication.class, args);
	}

}
