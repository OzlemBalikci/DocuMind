package com.documind.backend.document;

import com.documind.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DocumentChunk extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "document_id", nullable = false)
	private Document document;

	@Column(name = "chunk_index", nullable = false)
	private Integer chunkIndex;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "token_count")
	private Integer tokenCount;

	@Column(name = "page_number")
	private Integer pageNumber;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
	@Builder.Default
	private Map<String, Object> metadata = new HashMap<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

}
