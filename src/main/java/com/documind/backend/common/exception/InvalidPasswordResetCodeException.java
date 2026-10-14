package com.documind.backend.common.exception;

public class InvalidPasswordResetCodeException extends ApiException {

	public InvalidPasswordResetCodeException() {
		super(ErrorCode.INVALID_PASSWORD_RESET_CODE);
	}
}
