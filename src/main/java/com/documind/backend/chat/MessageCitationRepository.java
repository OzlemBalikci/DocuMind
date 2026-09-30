package com.documind.backend.chat;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, Long> {

	@EntityGraph(attributePaths = { "chunk", "chunk.document" })
	List<MessageCitation> findByMessage_IdOrderByRankOrderAsc(Long messageId);

}
