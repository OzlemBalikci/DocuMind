package com.documind.backend.common.exception;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
