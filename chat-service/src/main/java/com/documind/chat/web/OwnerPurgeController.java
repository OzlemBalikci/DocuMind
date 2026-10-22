package com.documind.chat.web;

import com.documind.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/owners")
@RequiredArgsConstructor
public class OwnerPurgeController {

	private final ConversationRepository conversations;

	@DeleteMapping("/{ownerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purge(@PathVariable UUID ownerId) {
		conversations.deleteByOwnerId(ownerId);
	}
}
