package com.documind.backend.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

	Optional<DocumentChunk> findByDocumentIdAndChunkIndex(Long documentId, Integer chunkIndex);

	long countByDocumentId(Long documentId);

	void deleteByDocument_Id(Long documentId);

}
