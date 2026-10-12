package com.documind.backend.auth.repository;

import com.documind.backend.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findLockedByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
