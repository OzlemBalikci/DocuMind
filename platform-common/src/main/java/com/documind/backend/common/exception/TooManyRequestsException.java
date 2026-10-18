package com.documind.backend.common.exception;

import java.time.Duration;

public class TooManyRequestsException extends ApiException {

	private final Duration retryAfter;

	public TooManyRequestsException(Duration retryAfter) {
		super(ErrorCode.TOO_MANY_REQUESTS);
		this.retryAfter = retryAfter == null || retryAfter.isNegative() ? Duration.ofSeconds(1) : retryAfter;
	}

	public Duration getRetryAfter() {
		return retryAfter;
	}
}
