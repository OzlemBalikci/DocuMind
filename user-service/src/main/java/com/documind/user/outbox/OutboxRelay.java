package com.documind.user.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class OutboxRelay {

	private final OutboxRepository outbox;
	private final List<OutboxEventHandler> handlers;
	private final TransactionTemplate transaction;
	private final int batchSize;
	private final int maxAttempts;

	public OutboxRelay(
			OutboxRepository outbox,
			List<OutboxEventHandler> handlers,
			PlatformTransactionManager transactionManager,
			@Value("${app.outbox.batch-size:20}") int batchSize,
			@Value("${app.outbox.max-attempts:10}") int maxAttempts) {
		this.outbox = outbox;
		this.handlers = handlers;
		this.transaction = new TransactionTemplate(transactionManager);
		this.batchSize = batchSize;
		this.maxAttempts = maxAttempts;
	}

	private record Claim(UUID id, OutboxEvent.Type type, UUID aggregateId) {
	}

	@Scheduled(fixedDelayString = "${app.outbox.poll-interval:2s}")
	public void relay() {
		List<Claim> claims = transaction.execute(status -> {
			List<OutboxEvent> due = outbox.findDue(Instant.now(), PageRequest.of(0, batchSize));
			due.forEach(OutboxEvent::markProcessing);
			return due.stream()
					.map(event -> new Claim(event.getId(), event.getEventType(), event.getAggregateId()))
					.toList();
		});
		if (claims == null) {
			return;
		}
		for (Claim claim : claims) {
			process(claim);
		}
	}

	private void process(Claim claim) {
		try {
			OutboxEvent event = outbox.findById(claim.id()).orElse(null);
			if (event == null) {
				return;
			}
			for (OutboxEventHandler handler : handlers) {
				if (handler.supports(claim.type())) {
					handler.handle(event);
				}
			}
			transaction.executeWithoutResult(status ->
					outbox.findById(claim.id()).ifPresent(OutboxEvent::markProcessed));
		} catch (RuntimeException ex) {
			log.warn("Outbox {} failed", claim.id(), ex);
			transaction.executeWithoutResult(status -> outbox.findById(claim.id()).ifPresent(event -> {
				event.markFailed(truncate(ex.getMessage()), retryAt(event.getAttempts() + 1), maxAttempts);
				if (event.getStatus() == OutboxStatus.DEAD) {
					log.error("Outbox {} moved to DEAD after {} attempts", event.getId(), event.getAttempts());
				}
			}));
		}
	}

	private static Instant retryAt(int attempts) {
		long seconds = Math.min(300, (long) Math.pow(2, Math.min(attempts, 8)));
		return Instant.now().plus(Duration.ofSeconds(seconds));
	}

	private static String truncate(String message) {
		if (message == null) {
			return "unknown error";
		}
		return message.length() > 1000 ? message.substring(0, 1000) : message;
	}
}
