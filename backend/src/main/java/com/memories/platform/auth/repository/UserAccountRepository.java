package com.memories.platform.auth.repository;

import com.memories.platform.auth.entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user
            from UserAccount user
            where user.email = :email
              and user.deletedAt is null
            """)
    Optional<UserAccount> findForUpdateByEmail(
            @Param("email") String email
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user where user.id = :userId")
    Optional<UserAccount> findForUpdateById(@Param("userId") UUID userId);
}
