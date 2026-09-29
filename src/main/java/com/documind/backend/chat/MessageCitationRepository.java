package com.documind.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, Long> {

    List<MessageCitation> findByMessage_IdOrderByRankOrderAsc(Long messageId);

    void deleteByMessage_Id(Long messageId);
}
