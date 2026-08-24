package com.memories.platform.ratelimit.repository;

import com.memories.platform.ratelimit.constants.RateLimitScope;
import com.memories.platform.ratelimit.entity.RateLimitBucket;
import com.memories.platform.ratelimit.entity.RateLimitBucketId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, RateLimitBucketId> {

    @Modifying
    @Query(value = """
            INSERT INTO rate_limit_buckets (
                scope,
                subject_key,
                window_started_at,
                request_count,
                updated_at
            ) VALUES (:scope, :subjectKey, :now, 0, :now)
            ON CONFLICT (scope, subject_key) DO NOTHING
            """, nativeQuery = true)
    int initialize(
            @Param("scope") String scope,
            @Param("subjectKey") String subjectKey,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bucket
            from RateLimitBucket bucket
            where bucket.scope = :scope
              and bucket.subjectKey = :subjectKey
            """)
    Optional<RateLimitBucket> findForUpdate(
            @Param("scope") RateLimitScope scope,
            @Param("subjectKey") String subjectKey
    );

    @Modifying
    @Query("delete from RateLimitBucket bucket where bucket.updatedAt < :threshold")
    int deleteStale(@Param("threshold") Instant threshold);
}
