package com.sirket.auth.auth.service.impl;

import com.sirket.auth.common.config.PasswordResetProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class PasswordResetCodeHasher {

	private static final int MIN_PEPPER_LENGTH = 32;
	private static final String HMAC_SHA256 = "HmacSHA256";

	private final byte[] pepper;

	@Autowired
	public PasswordResetCodeHasher(PasswordResetProperties properties) {
		this(properties.getPepper());
	}

	private PasswordResetCodeHasher(String pepper) {
		if (pepper == null || pepper.length() < MIN_PEPPER_LENGTH) {
			throw new IllegalStateException(
					"app.auth.password-reset.pepper must be at least " + MIN_PEPPER_LENGTH + " characters");
		}
		this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
	}

	public String hmac(String code) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(pepper, HMAC_SHA256));
			return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HmacSHA256 is unavailable", ex);
		}
	}
}
