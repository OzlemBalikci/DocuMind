package com.documind.backend.document.repository;

import com.documind.backend.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(Long documentId);

	Optional<DocumentChunk> findByDocument_IdAndChunkIndex(Long documentId, Integer chunkIndex);

	long countByDocument_Id(Long documentId);

	@Modifying
	@Transactional
	void deleteByDocument_Id(Long documentId);

}
