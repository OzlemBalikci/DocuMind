package com.documind.backend.common.exception;

public class NotFoundException extends ApiException {
    public NotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }
}
