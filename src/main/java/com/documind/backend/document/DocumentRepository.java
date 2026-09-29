package com.documind.backend.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndUser_Id(Long id, Long userId);

    Page<Document> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Document> findByUser_IdAndChecksumSha256(Long userId, String checksumSha256);

    boolean existsByUser_IdAndChecksumSha256(Long userId, String checksumSha256);

    List<Document> findByStatusOrderByCreatedAtAsc(DocumentStatus status);

    long countByUser_Id(Long userId);
}
