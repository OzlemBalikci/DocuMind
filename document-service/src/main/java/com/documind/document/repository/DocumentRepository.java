package com.documind.document.repository;

import com.documind.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

	List<Document> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	Optional<Document> findByIdAndOwnerId(UUID id, UUID ownerId);

	@Transactional
	long deleteByOwnerId(UUID ownerId);
}
