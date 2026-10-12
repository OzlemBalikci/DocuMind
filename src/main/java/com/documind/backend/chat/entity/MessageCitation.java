package com.documind.backend.chat.entity;

import com.documind.backend.common.entity.BaseEntity;
import com.documind.backend.document.entity.DocumentChunk;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "message_citations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageCitation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "chunk_id", nullable = false)
	private DocumentChunk chunk;

	@Column(name = "rank_order", nullable = false)
	private Short rankOrder;

	@Column(nullable = false, precision = 6, scale = 5)
	private BigDecimal similarity;

	public MessageCitation(Message message, DocumentChunk chunk, int rank, BigDecimal similarity) {
		this.message = message;
		this.chunk = chunk;
		this.rankOrder = (short) rank;
		this.similarity = similarity;
	}

}
