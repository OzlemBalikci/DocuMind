package com.documind.chat.web;

import com.documind.chat.JwtDecoderTestConfig;
import com.documind.chat.entity.Conversation;
import com.documind.chat.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JwtDecoderTestConfig.class)
class ConversationControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ConversationRepository conversations;

	@Test
	void missingTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/chat/conversations"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void otherUsersConversationIsNotFound() throws Exception {
		UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
		UUID stranger = UUID.fromString("22222222-2222-2222-2222-222222222222");
		Conversation stored = conversations.save(Conversation.create(owner, "Mine"));
		mockMvc.perform(get("/api/chat/conversations/" + stored.getId()).with(jwt().jwt(jwt -> jwt
						.subject(stranger.toString())
						.claim("email", "stranger@example.com")
						.claim("realm_access", Map.of("roles", List.of("USER"))))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isNotFound());
	}
}
