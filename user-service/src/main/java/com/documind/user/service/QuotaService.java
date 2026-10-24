package com.documind.user.service;

import com.documind.user.dto.response.QuotaResponse;

import java.util.UUID;

public interface QuotaService {

	QuotaResponse consume(UUID profileId, int amount, UUID operationId);

	QuotaResponse release(UUID profileId, int amount, UUID operationId);
}
