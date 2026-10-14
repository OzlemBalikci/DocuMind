package com.documind.backend.common.exception;

public class FileTooLargeException extends ApiException {
    public FileTooLargeException () {
        super(ErrorCode.FILE_TOO_LARGE);
    }
}
