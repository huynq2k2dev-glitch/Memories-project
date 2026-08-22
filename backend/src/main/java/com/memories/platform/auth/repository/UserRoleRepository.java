package com.memories.platform.auth.repository;

import com.memories.platform.auth.entity.UserRole;
import com.memories.platform.auth.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM user_roles user_role
                JOIN role_permissions role_permission ON role_permission.role_id = user_role.role_id
                JOIN permissions permission ON permission.id = role_permission.permission_id
                JOIN users user_account ON user_account.id = user_role.user_id
                WHERE user_role.user_id = :userId
                  AND permission.code = :permissionCode
                  AND user_account.status = 'ACTIVE'
                  AND user_account.deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean activeUserHasPermission(
            @Param("userId") UUID userId,
            @Param("permissionCode") String permissionCode
    );
}
