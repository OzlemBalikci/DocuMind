package com.documind.chat.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/conversations")
public class ConversationController {

	@GetMapping
	public List<ConversationResponse> list() {
		return List.of();
	}

	public record ConversationResponse(Long id, String title) {
	}
}
