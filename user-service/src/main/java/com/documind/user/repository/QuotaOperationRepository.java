package com.documind.user.repository;

import com.documind.user.model.QuotaOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface QuotaOperationRepository extends JpaRepository<QuotaOperation, UUID> {

	@Modifying
	@Transactional
	@Query(value = """
			insert into quota_operations (operation_id, user_id, amount, type, created_at)
			values (:id, :userId, :amount, :type, now())
			on conflict (operation_id) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("userId") UUID userId,
			@Param("amount") int amount,
			@Param("type") String type);
}
