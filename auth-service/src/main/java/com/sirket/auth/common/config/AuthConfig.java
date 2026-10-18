package com.sirket.auth.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthCookieProperties.class, PasswordResetProperties.class, MailProperties.class})
public class AuthConfig {
}
