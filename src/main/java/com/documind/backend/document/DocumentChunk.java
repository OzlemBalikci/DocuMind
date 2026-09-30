package com.documind.backend.document;

import com.documind.backend.common.CreatedOnlyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "document_chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk extends CreatedOnlyEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(name = "chunk_index", nullable = false)
	private Integer chunkIndex;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "token_count")
	private Integer tokenCount;

	@Column(name = "page_number")
	private Integer pageNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> metadata = new HashMap<>();

	@Builder
	private DocumentChunk(Document document, Integer chunkIndex, String content, Integer tokenCount,
			Integer pageNumber, Map<String, Object> metadata) {
		this.document = document;
		this.chunkIndex = chunkIndex;
		this.content = content;
		this.tokenCount = tokenCount;
		this.pageNumber = pageNumber;
		if (metadata != null) {
			this.metadata = metadata;
		}
	}

}
