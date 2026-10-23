package com.documind.user.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

	boolean existsByAggregateIdAndEventType(UUID aggregateId, OutboxEvent.Type eventType);

	Optional<OutboxEvent> findByAggregateIdAndEventType(UUID aggregateId, OutboxEvent.Type eventType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
	@Query("""
			select e from OutboxEvent e
			 where e.status in (com.documind.user.outbox.OutboxStatus.PENDING,
			                    com.documind.user.outbox.OutboxStatus.FAILED)
			   and e.availableAt <= :now
			 order by e.createdAt
			""")
	List<OutboxEvent> findDue(@Param("now") Instant now, Pageable page);

	@Query("""
			select e from OutboxEvent e
			 where e.status = com.documind.user.outbox.OutboxStatus.PROCESSING
			   and e.availableAt <= :stuckBefore
			""")
	List<OutboxEvent> findStuckProcessing(@Param("stuckBefore") Instant stuckBefore);
}
