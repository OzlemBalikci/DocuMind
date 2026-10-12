package com.documind.backend.common.exception;

import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;

public class UnsupportedFileTypeException extends ApiException {
    public UnsupportedFileTypeException () {
        super(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
