package com.memories.platform.auth.repository;

import com.memories.platform.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("select refreshToken.user.id from RefreshToken refreshToken where refreshToken.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select refreshToken
            from RefreshToken refreshToken
            join fetch refreshToken.user
            where refreshToken.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update RefreshToken refreshToken
            set refreshToken.revokedAt = :revokedAt,
                refreshToken.revocationReason = :reason
            where refreshToken.familyId = :familyId
              and refreshToken.revokedAt is null
            """)
    int revokeActiveFamily(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason
    );

    @Modifying
    @Query("""
            update RefreshToken refreshToken
            set refreshToken.revokedAt = :revokedAt,
                refreshToken.revocationReason = :reason
            where refreshToken.user.id = :userId
              and refreshToken.revokedAt is null
            """)
    int revokeAllActiveForUser(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") String reason
    );
}
