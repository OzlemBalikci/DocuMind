package com.documind.chat.repository;

import com.documind.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	List<Conversation> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

	Optional<Conversation> findByIdAndOwnerId(UUID id, UUID ownerId);

	@Transactional
	long deleteByOwnerId(UUID ownerId);
}
