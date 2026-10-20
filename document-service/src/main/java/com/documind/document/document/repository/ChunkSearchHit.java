package com.documind.document.document.repository;


public interface ChunkSearchHit {
    Long getChunkId();
    Long getDocumentId();
    String getTitle();
    Integer getChunkIndex();
    Integer getPageNumber();
    String getContent();
    Double getScore();
}
