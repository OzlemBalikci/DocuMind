package com.documind.backend.common.exception;

public class InvalidRefreshTokenException extends ApiException {
    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}