package com.documind.user.outbox;

public interface OutboxEventHandler {

	boolean supports(OutboxEvent.Type type);

	void handle(OutboxEvent event);
}
