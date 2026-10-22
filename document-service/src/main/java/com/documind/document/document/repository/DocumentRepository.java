package com.documind.document.document.repository;

import com.documind.document.document.domain.DocumentStatus;
import com.documind.document.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

	Optional<Document> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<Document> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	Page<DocumentSummary> findByOwnerId(UUID ownerId, Pageable pageable);

	Optional<Document> findByOwnerIdAndChecksumSha256(UUID ownerId, String checksumSha256);

	boolean existsByOwnerIdAndChecksumSha256(UUID ownerId, String checksumSha256);

	@Transactional
	long deleteByOwnerId(UUID ownerId);

	@Modifying
	@Query("delete from Document d where d.id = :id and d.ownerId = :ownerId")
	int deleteOwned(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

	@Query(value = """
			SELECT * FROM documents
			 WHERE status = 'PENDING'
			 ORDER BY created_at
			 LIMIT :limit
			 FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<Document> lockNextPending(@Param("limit") int limit);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Document d
			   set d.status = :to, d.updatedAt = :now
			 where d.id = :id and d.status = :from
			""")
	int transition(@Param("id") UUID id,
			@Param("from") DocumentStatus from,
			@Param("to") DocumentStatus to,
			@Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Document d
			   set d.status = :embedding, d.pageCount = :pageCount, d.updatedAt = :now
			 where d.id = :id and d.status = :parsing
			""")
	int markEmbedding(@Param("id") UUID id,
			@Param("pageCount") Integer pageCount,
			@Param("now") Instant now,
			@Param("parsing") DocumentStatus parsing,
			@Param("embedding") DocumentStatus embedding);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Document d
			   set d.status = :ready,
			       d.chunkCount = :chunkCount,
			       d.embeddingModel = :model,
			       d.indexedAt = :now,
			       d.updatedAt = :now,
			       d.errorMessage = null
			 where d.id = :id and d.status = :embedding
			""")
	int markReady(@Param("id") UUID id,
			@Param("chunkCount") int chunkCount,
			@Param("model") String model,
			@Param("now") Instant now,
			@Param("embedding") DocumentStatus embedding,
			@Param("ready") DocumentStatus ready);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Document d
			   set d.status = :failed, d.errorMessage = :message, d.updatedAt = :now
			 where d.id = :id and d.status in :active
			""")
	int markFailed(@Param("id") UUID id,
			@Param("message") String message,
			@Param("now") Instant now,
			@Param("failed") DocumentStatus failed,
			@Param("active") Collection<DocumentStatus> active);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update Document d
			   set d.status = :pending, d.errorMessage = null, d.updatedAt = :now
			 where d.id = :id and d.ownerId = :ownerId and d.status = :failed
			""")
	int retryFailed(@Param("id") UUID id,
			@Param("ownerId") UUID ownerId,
			@Param("now") Instant now,
			@Param("failed") DocumentStatus failed,
			@Param("pending") DocumentStatus pending);

	@Query("""
			select d.id from Document d
			 where d.status in :statuses and d.updatedAt < :before
			""")
	List<UUID> findStuckIds(@Param("statuses") Collection<DocumentStatus> statuses,
			@Param("before") Instant before);

	@Query("""
			select d.id from Document d
			 where d.status = :ready
			   and (d.embeddingModel is null or d.embeddingModel <> :currentModel)
			""")
	List<UUID> findIdsWithOutdatedModel(@Param("currentModel") String currentModel,
			@Param("ready") DocumentStatus ready);
}
