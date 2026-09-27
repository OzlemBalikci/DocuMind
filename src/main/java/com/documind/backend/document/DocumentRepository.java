package com.documind.backend.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document , Long> {


    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.user.id = :userId")
    Optional<Document> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Page<Document> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Document> findByUserIdAndChecksumSha256(Long userId, String checksumSha256);

    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.createdAt ASC")
    List<Document> findByStatusOrderByCreatedAtAsc(@Param("status") DocumentStatus status);
}
