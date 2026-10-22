package com.documind.chat.web;

import com.documind.backend.common.exception.NotFoundException;
import com.documind.chat.entity.Conversation;
import com.documind.chat.entity.Message;
import com.documind.chat.repository.ConversationRepository;
import com.documind.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat/conversations")
@RequiredArgsConstructor
public class ConversationController {

	private final ConversationRepository conversations;
	private final MessageRepository messages;

	@GetMapping
	public List<ConversationResponse> list(@AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = UUID.fromString(jwt.getSubject());
		return conversations.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream()
				.map(ConversationResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	public ConversationResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return owned(id, jwt);
	}

	@GetMapping("/{id}/messages")
	public List<MessageResponse> messages(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		owned(id, jwt);
		return messages.findByConversationIdOrderByCreatedAtAsc(id).stream()
				.map(MessageResponse::from)
				.toList();
	}

	private ConversationResponse owned(UUID id, Jwt jwt) {
		UUID ownerId = UUID.fromString(jwt.getSubject());
		return conversations.findByIdAndOwnerId(id, ownerId)
				.map(ConversationResponse::from)
				.orElseThrow(NotFoundException::new);
	}

	public record ConversationResponse(UUID id, String title) {
		static ConversationResponse from(Conversation conversation) {
			return new ConversationResponse(conversation.getId(), conversation.getTitle());
		}
	}

	public record MessageResponse(UUID id, String role, String content, String citations) {
		static MessageResponse from(Message message) {
			return new MessageResponse(
					message.getId(),
					message.getRole(),
					message.getContent(),
					message.getCitations());
		}
	}
}
