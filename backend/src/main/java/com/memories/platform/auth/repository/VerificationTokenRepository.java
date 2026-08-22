package com.memories.platform.auth.repository;

import com.memories.platform.auth.entity.VerificationToken;
import com.memories.platform.auth.entity.VerificationTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from VerificationToken token
            join fetch token.user
            where token.tokenHash = :tokenHash
              and token.type = :type
            """)
    Optional<VerificationToken> findForUpdateByTokenHashAndType(
            @Param("tokenHash") String tokenHash,
            @Param("type") VerificationTokenType type
    );

    List<VerificationToken> findAllByUserAndTypeAndUsedAtIsNull(
            com.memories.platform.auth.entity.UserAccount user,
            VerificationTokenType type
    );
}
