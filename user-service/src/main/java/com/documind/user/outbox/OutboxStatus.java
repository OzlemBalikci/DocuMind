package com.documind.user.outbox;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD
}
