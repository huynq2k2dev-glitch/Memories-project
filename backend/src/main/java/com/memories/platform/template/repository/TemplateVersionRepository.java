package com.memories.platform.template.repository;

import com.memories.platform.template.entity.TemplateVersion;
import com.memories.platform.template.entity.TemplateVersionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    @Query("""
            select coalesce(max(templateVersion.versionNo), 0)
            from TemplateVersion templateVersion
            where templateVersion.template.id = :templateId
            """)
    int maximumVersionNumber(@Param("templateId") UUID templateId);

    @Query("""
            select templateVersion
            from TemplateVersion templateVersion
            join fetch templateVersion.template
            where templateVersion.template.id in :templateIds
              and templateVersion.status = :status
            order by templateVersion.template.id, templateVersion.versionNo desc
            """)
    List<TemplateVersion> findForCatalog(
            @Param("templateIds") List<UUID> templateIds,
            @Param("status") TemplateVersionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select templateVersion
            from TemplateVersion templateVersion
            join fetch templateVersion.template
            where templateVersion.id = :versionId
            """)
    Optional<TemplateVersion> findForSelection(@Param("versionId") UUID versionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select templateVersion
            from TemplateVersion templateVersion
            join fetch templateVersion.template
            where templateVersion.id = :versionId
              and templateVersion.template.id = :templateId
            """)
    Optional<TemplateVersion> findForUpdate(
            @Param("templateId") UUID templateId,
            @Param("versionId") UUID versionId
    );
}
