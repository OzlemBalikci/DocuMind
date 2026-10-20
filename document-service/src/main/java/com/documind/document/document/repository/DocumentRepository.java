package com.documind.document.document.repository;

import com.documind.document.document.domain.DocumentStatus;
import com.documind.document.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndUserId(Long id, Long userId);

    Page<DocumentSummary> findByUserId(Long userId, Pageable pageable);

    Optional<Document> findByUserIdAndChecksumSha256(Long userId, String checksumSha256);

    boolean existsByUserIdAndChecksumSha256(Long userId, String checksumSha256);

    @Modifying
    @Query("delete from Document  d where d.id = :id and d.userId = :userId")
    int deleteOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Query(value = """
          SELECT  * FROM documents
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
    int transition(@Param("id") Long id,
                   @Param("from")DocumentStatus from,
                   @Param("to") DocumentStatus to,
                   @Param("now")Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
          update Document d
            set d.status = :embedding, d.pageCount = :pageCount, d.updatedAt = :now
          where d.id = :id and d.status = :parsing
    """)
    int markEmbedding(@Param("id") Long id,
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
    int markReady(@Param("id") Long id,
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
    int markFailed(@Param("id") Long id,
                   @Param("message") String message,
                   @Param("now") Instant now,
                   @Param("failed") DocumentStatus failed,
                   @Param("active") Collection<DocumentStatus> active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Document d
               set d.status = :pending, d.errorMessage = null, d.updatedAt = :now
             where d.id = :id and d.userId = :userId and d.status = :failed
            """)
    int retryFailed(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("now") Instant now,
                    @Param("failed") DocumentStatus failed,
                    @Param("pending") DocumentStatus pending);

    @Query("""
            select d.id from Document d
             where d.status in :statuses and d.updatedAt < :before
            """)
    List<Long> findStuckIds(@Param("statuses") Collection<DocumentStatus> statuses,
                            @Param("before") Instant before);

    @Query("""
            select d.id from Document d
             where d.status = :ready
               and (d.embeddingModel is null or d.embeddingModel <> :currentModel)
            """)
    List<Long> findIdsWithOutdatedModel(@Param("currentModel") String currentModel,
                                        @Param("ready") DocumentStatus ready);






}
