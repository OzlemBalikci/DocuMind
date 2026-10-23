package com.documind.user.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class OutboxReconciler {

	private final OutboxRepository outbox;
	private final Duration stuckAfter;

	public OutboxReconciler(
			OutboxRepository outbox,
			@Value("${app.outbox.stuck-after:10m}") Duration stuckAfter) {
		this.outbox = outbox;
		this.stuckAfter = stuckAfter;
	}

	@Scheduled(fixedDelayString = "${app.outbox.reconcile-interval:30s}")
	@Transactional
	public void reconcile() {
		Instant cutoff = Instant.now().minus(stuckAfter);
		List<OutboxEvent> stuck = outbox.findStuckProcessing(cutoff);
		for (OutboxEvent event : stuck) {
			log.warn("Recovering stuck outbox event {} for aggregate {}",
					event.getId(), event.getAggregateId());
			event.recover();
		}
	}
}
