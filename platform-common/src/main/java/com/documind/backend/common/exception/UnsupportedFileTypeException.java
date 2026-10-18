package com.documind.backend.common.exception;

public class UnsupportedFileTypeException extends ApiException {
    public UnsupportedFileTypeException () {
        super(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
