package com.documind.document.document.repository;

import java.util.UUID;

public interface ChunkSearchHit {
	UUID getChunkId();
	UUID getDocumentId();
	String getTitle();
	Integer getChunkIndex();
	Integer getPageNumber();
	String getContent();
	Double getScore();
}
