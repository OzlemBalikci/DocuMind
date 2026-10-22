package com.documind.document.web;

import com.documind.document.repository.DocumentRepository;
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

	private final DocumentRepository documents;

	@DeleteMapping("/{ownerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purge(@PathVariable UUID ownerId) {
		documents.deleteByOwnerId(ownerId);
	}
}
