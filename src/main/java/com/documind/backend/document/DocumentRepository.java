package com.documind.backend.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	Optional<Document> findByIdAndUser_Id(Long id, Long userId);

	Page<Document> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	Optional<Document> findByUser_IdAndChecksumSha256(Long userId, String checksumSha256);

	boolean existsByUser_IdAndChecksumSha256(Long userId, String checksumSha256);

	@Modifying(clearAutomatically = true)
	@Query("""
			update Document d set d.status = com.documind.backend.document.DocumentStatus.FAILED,
			                      d.errorMessage = 'Zaman aşımı',
			                      d.updatedAt = :now
			where d.status in (com.documind.backend.document.DocumentStatus.PARSING,
			                   com.documind.backend.document.DocumentStatus.EMBEDDING)
			  and d.updatedAt < :threshold
			""")
	int failStuck(@Param("threshold") Instant threshold, @Param("now") Instant now);

}
