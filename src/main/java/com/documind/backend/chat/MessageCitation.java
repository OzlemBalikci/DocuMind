package com.documind.backend.chat;

import com.documind.backend.common.BaseEntity;
import com.documind.backend.document.DocumentChunk;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "message_citations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageCitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "message_id",nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Column(name = "rank_order", nullable = false)
    private Short rankOrder;

    @Column(name = "similarity", nullable = false, precision = 6, scale = 5)
    private BigDecimal similarity;
}
