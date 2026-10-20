package com.documind.document.document.repository;

import com.documind.document.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;
import java.util.Collection;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DocumentChunk c where c.document.id = :documentId")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DocumentChunk c set c.embedding = null where c.document.id = :documentId")
    int clearEmbeddings(@Param("documentId") Long documentId);

    @Query("""
            select c from DocumentChunk  c
            where c.document.id = :documentId and c.embedding is null
            order by c.chunkIndex
           """)
    List<DocumentChunk> findPendingEmbedding(@Param("documentId") Long documentId, Pageable pageable);

    @Query("select count(c) from DocumentChunk c where c.document.id = :documentId and c.embedding is null")
    long countPendingEmbedding(@Param("documentId") Long documentId);

    long countByDocumentId(Long documentId);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    List<DocumentChunk> findByDocumentIdAndChunkIndexBetweenOrderByChunkIndex(Long documentId, int fromIndex, int toIndex);

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
             WHERE d.user_id = :userId
               AND d.status = 'READY'
               AND c.embedding IS NOT NULL
             ORDER BY c.embedding <=> CAST(:queryVector AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<ChunkSearchHit> searchByUser(@Param("userId") Long userId,
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
             WHERE d.user_id = :userId
               AND d.status = 'READY'
               AND c.document_id IN (:documentIds)
               AND c.embedding IS NOT NULL
             ORDER BY c.embedding <=> CAST(:queryVector AS vector)
             LIMIT :limit
            """, nativeQuery = true)
    List<ChunkSearchHit> searchInDocuments(@Param("userId") Long userId,
                                           @Param("documentIds") Collection<Long> documentIds,
                                           @Param("queryVector") String queryVector,
                                           @Param("limit") int limit);

    default List<ChunkSearchHit> searchByUser(Long userId, float[] queryVector, int limit) {
        return searchByUser(userId, toVectorLiteral(queryVector), limit);
    }

    default List<ChunkSearchHit> searchInDocuments(Long userId, Collection<Long> documentIds,
                                                   float[] queryVector, int limit) {
        return searchInDocuments(userId, documentIds, toVectorLiteral(queryVector), limit);
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
