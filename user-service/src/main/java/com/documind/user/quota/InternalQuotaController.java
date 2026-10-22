package com.documind.user.quota;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/profiles")
public class InternalQuotaController {

	private final QuotaService quota;

	public InternalQuotaController(QuotaService quota) {
		this.quota = quota;
	}

	@PostMapping("/{id}/quota/consume")
	public QuotaResponse consume(@PathVariable UUID id, @Valid @RequestBody QuotaAmountRequest body) {
		return quota.consume(id, body.amount(), body.operationId());
	}

	@PostMapping("/{id}/quota/release")
	public QuotaResponse release(@PathVariable UUID id, @Valid @RequestBody QuotaAmountRequest body) {
		return quota.release(id, body.amount(), body.operationId());
	}
}
