package com.documind.user.outbox;

import com.documind.user.model.OutboxEvent;

public interface OutboxEventHandler {

	boolean supports(OutboxEvent.Type type);

	void handle(OutboxEvent event);
}
