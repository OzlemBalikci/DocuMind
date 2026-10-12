package com.documind.backend.chat.repository;

import com.documind.backend.chat.entity.Conversation;
import com.documind.backend.chat.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

	List<Message> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

	Optional<Message> findByIdAndConversation_Id(Long id, Long conversationId);

	@Query("""
			SELECT m FROM Message m
			WHERE m.id = :id AND m.conversation.user.id = :userId
			""")
	Optional<Message> findByIdAndConversation_User_Id(@Param("id") Long id, @Param("userId") Long userId);

}
