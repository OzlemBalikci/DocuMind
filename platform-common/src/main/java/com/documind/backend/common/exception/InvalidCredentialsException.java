package com.documind.backend.common.exception;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
