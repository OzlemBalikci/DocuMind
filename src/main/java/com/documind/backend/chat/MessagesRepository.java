package com.documind.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessagesRepository extends JpaRepository<Messages, Long> {

	List<Messages> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

	Optional<Messages> findByIdAndConversation_Id(Long id, Long conversationId);

	@Query("""
			SELECT m FROM Messages m
			WHERE m.id = :id AND m.conversation.user.id = :userId
			""")
	Optional<Messages> findByIdAndConversation_User_Id(@Param("id") Long id, @Param("userId") Long userId);

	List<Messages> findByStatusOrderByCreatedAtAsc(MessagesStatus status);

	long countByConversation_Id(Long conversationId);

}
