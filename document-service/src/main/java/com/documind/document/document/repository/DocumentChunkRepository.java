package com.documind.document.document.repository;

import com.documind.document.document.entity.DocumentChunk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DocumentChunk c where c.document.id = :documentId")
    int deleteByDocumentId(@Param("documentId") UUID documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DocumentChunk c set c.embedding = null where c.document.id = :documentId")
    int clearEmbeddings(@Param("documentId") UUID documentId);

    @Query("""
            select c from DocumentChunk  c
            where c.document.id = :documentId and c.embedding is null
            order by c.chunkIndex
           """)
    List<DocumentChunk> findPendingEmbedding(@Param("documentId") UUID documentId, Pageable pageable);

    @Query("select count(c) from DocumentChunk c where c.document.id = :documentId and c.embedding is null")
    long countPendingEmbedding(@Param("documentId") UUID documentId);

    long countByDocumentId(UUID documentId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    List<DocumentChunk> findByDocumentIdAndChunkIndexBetweenOrderByChunkIndex(UUID documentId, int fromIndex, int toIndex);

    @Query(value = """
            SELECT c.id          AS "chunkId",
                   c.document_id AS "documentId",
                   d.title       AS "title",
                   c.chunk_index AS "chunkIndex",
                   c.page_number AS "pageNumber",
                   c.content     AS "content",
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS "score"
              FROM document_chunks c
              JOIN documents d ON d.id = c.document_id
             WHERE d.owner_id = :ownerId
               AND d.status = 'READY'
               AND c.embedding IS NOT NULL
             ORDER BY c.embedding <=> CAST(:queryVector AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<ChunkSearchHit> searchByUser(@Param("ownerId") UUID ownerId,
                                      @Param("queryVector") String queryVector,
                                      @Param("limit") int limit);

    @Query(value = """
            SELECT c.id          AS "chunkId",
                   c.document_id AS "documentId",
                   d.title       AS "title",
                   c.chunk_index AS "chunkIndex",
                   c.page_number AS "pageNumber",
                   c.content     AS "content",
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS "score"
              FROM document_chunks c
              JOIN documents d ON d.id = c.document_id
             WHERE d.owner_id = :ownerId
               AND d.status = 'READY'
               AND c.document_id IN (:documentIds)
               AND c.embedding IS NOT NULL
             ORDER BY c.embedding <=> CAST(:queryVector AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<ChunkSearchHit> searchInDocuments(@Param("ownerId") UUID ownerId,
                                           @Param("documentIds") Collection<UUID> documentIds,
                                           @Param("queryVector") String queryVector,
                                           @Param("limit") int limit);

    default List<ChunkSearchHit> searchByUser(UUID ownerId, float[] queryVector, int limit) {
        return searchByUser(ownerId, toVectorLiteral(queryVector), limit);
    }

    default List<ChunkSearchHit> searchInDocuments(UUID ownerId, Collection<UUID> documentIds,
                                                   float[] queryVector, int limit) {
        return searchInDocuments(ownerId, documentIds, toVectorLiteral(queryVector), limit);
    }

    static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }





}
