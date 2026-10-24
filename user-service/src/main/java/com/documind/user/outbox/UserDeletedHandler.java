package com.documind.user.outbox;

import com.documind.user.deletion.DeletionSteps;
import com.documind.user.model.OutboxEvent;
import org.springframework.stereotype.Component;

@Component
public class UserDeletedHandler implements OutboxEventHandler {

	private final DeletionSteps deletionSteps;

	public UserDeletedHandler(DeletionSteps deletionSteps) {
		this.deletionSteps = deletionSteps;
	}

	@Override
	public boolean supports(OutboxEvent.Type type) {
		return type == OutboxEvent.Type.USER_DELETED;
	}

	@Override
	public void handle(OutboxEvent event) {
		deletionSteps.execute(event.getAggregateId());
	}
}
