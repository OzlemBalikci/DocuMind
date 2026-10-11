package com.documind.backend.document.repository;

import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChunkVectorRepository {

	private static final int EMBEDDING_DIMENSIONS = 768;

	private final JdbcTemplate jdbc;

	public void saveEmbedding(long chunkId, float[] vector) {
		jdbc.update("UPDATE document_chunks SET embedding = ? WHERE id = ?", toPgVector(vector), chunkId);
	}

	public List<ChunkHit> search(long userId, float[] queryVector, int limit) {
		PGobject queryForSelect = toPgVector(queryVector);
		PGobject queryForOrder = toPgVector(queryVector);
		return jdbc.query("""
				SELECT c.id, c.document_id, c.content, c.page_number,
				       1 - (c.embedding <=> ?) AS similarity
				FROM document_chunks c
				JOIN documents d ON d.id = c.document_id
				WHERE d.user_id = ?
				  AND d.status = 'READY'
				  AND c.embedding IS NOT NULL
				ORDER BY c.embedding <=> ?
				LIMIT ?
				""", (rs, rowNum) -> new ChunkHit(rs.getLong("id"), rs.getLong("document_id"), rs.getString("content"),
				(Integer) rs.getObject("page_number"), rs.getDouble("similarity")), queryForSelect, userId, queryForOrder, limit);
	}

	private static PGobject toPgVector(float[] vector) {
		if (vector == null || vector.length != EMBEDDING_DIMENSIONS) {
			throw new IllegalArgumentException(
					"Embedding must be " + EMBEDDING_DIMENSIONS + " floats, got "
							+ (vector == null ? "null" : vector.length));
		}
		try {
			PGobject pg = new PGobject();
			pg.setType("vector");
			pg.setValue(toLiteral(vector));
			return pg;
		}
		catch (SQLException ex) {
			throw new IllegalStateException("Could not build pgvector parameter", ex);
		}
	}

	private static String toLiteral(float[] vector) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(vector[i]);
		}
		return sb.append(']').toString();
	}

	public record ChunkHit(long chunkId, long documentId, String content, Integer pageNumber, double similarity) {
	}

}
