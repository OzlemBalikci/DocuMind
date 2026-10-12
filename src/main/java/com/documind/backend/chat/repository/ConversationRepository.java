package com.documind.backend.chat.repository;

import com.documind.backend.chat.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

	Optional<Conversation> findByIdAndUser_Id(Long id, Long userId);

	Page<Conversation> findByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

}
