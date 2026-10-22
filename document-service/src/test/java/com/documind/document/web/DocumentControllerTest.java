package com.documind.document.web;

import com.documind.common.security.TestJwtSupport;
import com.documind.document.JwtDecoderTestConfig;
import com.documind.document.document.entity.Document;
import com.documind.document.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JwtDecoderTestConfig.class)
class DocumentControllerTest {

	private static final String CHECKSUM = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	DocumentRepository documents;

	@Test
	void missingTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/documents"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void otherUsersDocumentIsNotFound() throws Exception {
		UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
		UUID stranger = UUID.fromString("22222222-2222-2222-2222-222222222222");
		Document stored = documents.save(Document.pending(
				owner, "a.pdf", "a.pdf", "application/pdf", 12, CHECKSUM, "s3://a"));
		mockMvc.perform(get("/api/documents/" + stored.getId()).with(jwt().jwt(jwt -> jwt
						.subject(stranger.toString())
						.claim("email", "stranger@example.com")
						.claim("realm_access", Map.of("roles", List.of("USER"))))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void wrongAudienceIsUnauthorized() throws Exception {
		UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
		String token = TestJwtSupport.token(id, "user@example.com", "other-api", Instant.now(), "USER");
		mockMvc.perform(get("/api/documents")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}
}
