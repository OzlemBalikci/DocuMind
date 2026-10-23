package com.documind.user.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

	@Modifying
	@Transactional
	@Query(value = """
			-- Same id waits on this lock, so the email unique index still rejects a different user.
			insert into user_profiles (id, email, full_name, quota_limit, quota_used, created_at, updated_at)
			select :id, :email, :name, :limit, 0, now(), now()
			from (select pg_advisory_xact_lock(hashtextextended(cast(:id as text), 0))) as lock
			on conflict (id) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("email") String email,
			@Param("name") String name,
			@Param("limit") int limit);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
			update UserProfile p
			   set p.quotaUsed = p.quotaUsed + :amount, p.updatedAt = :now
			 where p.id = :id
			   and p.quotaUsed + :amount <= p.quotaLimit
			""")
	int consumeQuota(@Param("id") UUID id, @Param("amount") int amount, @Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
			update UserProfile p
			   set p.quotaUsed = p.quotaUsed - :amount, p.updatedAt = :now
			 where p.id = :id
			   and p.quotaUsed >= :amount
			""")
	int releaseQuota(@Param("id") UUID id, @Param("amount") int amount, @Param("now") Instant now);
}
