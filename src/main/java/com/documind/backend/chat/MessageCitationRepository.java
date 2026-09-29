package com.documind.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageCitationRepository extends JpaRepository<MessageCitation, Long> {

    List<MessageCitation> findByMessage_IdOrderByRankOrderAsc(Long messageId);

    @Modifying
    @Transactional
    void deleteByMessage_Id(Long messageId);
}
