package com.documind.document.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	@GetMapping
	public List<DocumentResponse> list() {
		return List.of();
	}

	public record DocumentResponse(Long id, String title, String status) {
	}
}
