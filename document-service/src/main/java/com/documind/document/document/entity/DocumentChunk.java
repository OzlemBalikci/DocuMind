package com.documind.document.document.entity;

import com.documind.common.persistence.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk extends CreatedOnlyEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(name = "chunk_index", nullable = false)
	private int chunkIndex;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "token_count")
	private Integer tokenCount;

	@Column(name = "page_number")
	private Integer pageNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String metadata;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = 768)
	@Column(name = "embedding")
	private float[] embedding;

	public static DocumentChunk create(
            Document document,
			int chunkIndex,
			String content,
			Integer tokenCount,
			Integer pageNumber) {
		DocumentChunk chunk = new DocumentChunk();
		chunk.document = document;
		chunk.chunkIndex = chunkIndex;
		chunk.content = content;
		chunk.tokenCount = tokenCount;
		chunk.pageNumber = pageNumber;
		chunk.metadata = "{}";
		return chunk;
	}

	public void assignEmbedding(float[] embedding) {
		this.embedding = embedding;
	}
}
