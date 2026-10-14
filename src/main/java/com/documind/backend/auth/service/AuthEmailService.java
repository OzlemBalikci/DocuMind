package com.documind.backend.auth.service;

import com.documind.backend.config.MailProperties;
import com.documind.backend.config.PasswordResetProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthEmailService {

	private final JavaMailSender mailSender;
	private final MailProperties mailProperties;
	private final PasswordResetProperties passwordResetProperties;

	public void sendPasswordResetCode(String toEmail, String code, Duration validFor) {
		if (mailProperties.getHost() == null || mailProperties.getHost().isBlank()) {
			throw new IllegalStateException("SMTP host is not configured (MAIL_HOST)");
		}
		if (mailProperties.getFrom() == null || mailProperties.getFrom().isBlank()) {
			throw new IllegalStateException("SMTP from address is not configured (MAIL_FROM)");
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
			helper.setFrom(mailProperties.getFrom());
			helper.setTo(toEmail);
			helper.setSubject(passwordResetProperties.getMailSubject());
			long minutes = Math.max(1, validFor.toMinutes());
			helper.setText("""
					You requested a password reset for your DocuMind account.

					Your verification code is: %s

					This code expires in %d minutes. If you did not request a reset, you can ignore this email.
					""".formatted(code, minutes));
			mailSender.send(message);
		} catch (MessagingException ex) {
			throw new IllegalStateException("Password reset email could not be built", ex);
		}
		log.debug("Password reset email sent to {}", toEmail);
	}
}
