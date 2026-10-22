package com.documind.document.document.service;

import com.documind.document.document.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {

	DocumentResponse upload(UUID ownerId, MultipartFile file, String title);

	Page<DocumentResponse> list(UUID ownerId, Pageable pageable);

	DocumentResponse get(UUID ownerId, UUID documentId);

	void delete(UUID ownerId, UUID documentId);
}
