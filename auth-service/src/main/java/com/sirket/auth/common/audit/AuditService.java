package com.sirket.auth.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

	public void authEvent(String action, Long userId, String ipAddress) {
		log.info("auth_audit action={} userId={} ip={}", action, userId, ipAddress);
	}
}
