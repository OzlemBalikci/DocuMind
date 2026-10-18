package com.sirket.auth.auth.repository;

import com.sirket.auth.auth.entity.PasswordResetCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

	Optional<PasswordResetCode> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

	@Query("""
			select c from PasswordResetCode c
			where c.user.id = :userId
			  and c.codeHash = :codeHash
			  and c.usedAt is null
			  and c.expiresAt > :now
			""")
	Optional<PasswordResetCode> findValidForUser(
			@Param("userId") Long userId,
			@Param("codeHash") String codeHash,
			@Param("now") Instant now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select c from PasswordResetCode c
			where c.user.id = :userId
			  and c.usedAt is null
			  and c.expiresAt > :now
			order by c.createdAt desc
			""")
	List<PasswordResetCode> findActiveForUserLocked(@Param("userId") Long userId, @Param("now") Instant now);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update PasswordResetCode c set c.usedAt = :now
			where c.user.id = :userId and c.usedAt is null
			""")
	int invalidateActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);

	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query("""
			update PasswordResetCode c set c.usedAt = :now
			where c.user.id = :userId
			  and c.usedAt is null
			  and c.expiresAt <= :now
			""")
	int invalidateExpiredForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
