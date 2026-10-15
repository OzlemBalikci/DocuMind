package com.documind.backend.common.exception;

public class QueueUnavailableException extends ApiException {
    public QueueUnavailableException () {
        super(ErrorCode.QUEUE_UNAVAILABLE);
    }
}
