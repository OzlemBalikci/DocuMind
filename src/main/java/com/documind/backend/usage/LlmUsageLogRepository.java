package com.documind.backend.usage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LlmUsageLogRepository extends JpaRepository<LlmUsageLog, Long> {

	Page<LlmUsageLog> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	List<LlmUsageLog> findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
			Long userId, Instant fromInclusive, Instant toInclusive);

	long countByUser_IdAndCreatedAtBetween(Long userId, Instant fromInclusive, Instant toInclusive);


	long countByCreatedAtBetween(Instant fromInclusive, Instant toInclusive);

	long countByUser_IdAndOperationAndCreatedAtBetween(
			Long userId, LlmOperation operation, Instant fromInclusive, Instant toInclusive);

	@Query("""
			SELECT COUNT(l) FROM LlmUsageLog l
			WHERE l.user.id = :userId
			  AND l.operation = :operation
			  AND l.success = true
			  AND l.createdAt >= :fromInclusive AND l.createdAt < :toExclusive
			""")
	long countSuccessfulByUserIdAndOperationAndCreatedAtRange(
			@Param("userId") Long userId,
			@Param("operation") LlmOperation operation,
			@Param("fromInclusive") Instant fromInclusive,
			@Param("toExclusive") Instant toExclusive);

}
