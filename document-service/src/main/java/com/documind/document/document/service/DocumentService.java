package com.documind.document.document.service;

import com.documind.document.document.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	DocumentResponse upload(long userId, MultipartFile file, String title);

	Page<DocumentResponse> list(long userId, Pageable pageable);

	DocumentResponse get(long userId, long documentId);

	void delete(long userId, long documentId);
}
