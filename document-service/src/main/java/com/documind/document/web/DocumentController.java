package com.documind.document.web;

import com.documind.backend.common.exception.NotFoundException;
import com.documind.document.document.entity.Document;
import com.documind.document.document.repository.DocumentRepository;
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
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentRepository documents;

	@GetMapping
	public List<DocumentResponse> list(@AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = UUID.fromString(jwt.getSubject());
		return documents.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
				.map(DocumentResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	public DocumentResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		UUID ownerId = UUID.fromString(jwt.getSubject());
		return documents.findByIdAndOwnerId(id, ownerId)
				.map(DocumentResponse::from)
				.orElseThrow(NotFoundException::new);
	}

	public record DocumentResponse(
			UUID id,
			String fileName,
			String contentType,
			long sizeBytes,
			String status) {

		static DocumentResponse from(Document document) {
			return new DocumentResponse(
					document.getId(),
					document.getOriginalFilename(),
					document.getContentType(),
					document.getSizeBytes(),
					document.getStatus().name());
		}
	}
}
