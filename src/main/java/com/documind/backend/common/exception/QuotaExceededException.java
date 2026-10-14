package com.documind.backend.common.exception;

public class QuotaExceededException extends ApiException {
    public QuotaExceededException () {
        super(ErrorCode.QUOTA_EXCEEDED);
    }
}
