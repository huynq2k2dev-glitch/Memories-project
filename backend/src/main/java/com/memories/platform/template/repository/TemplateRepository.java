package com.memories.platform.template.repository;

import com.memories.platform.template.entity.Template;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateRepository extends JpaRepository<Template, UUID> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "versions")
    List<Template> findAllByOrderByCreatedAtDesc();

    @Query("""
            select template
            from Template template
            where template.status = :status
              and (:memoryType is null or template.memoryType = :memoryType)
              and exists (
                  select 1
                  from TemplateVersion templateVersion
                  where templateVersion.template = template
                    and templateVersion.status = :versionStatus
              )
            """)
    Page<Template> findCatalog(
            @Param("status") com.memories.platform.template.entity.TemplateStatus status,
            @Param("memoryType") com.memories.platform.common.domain.MemoryType memoryType,
            @Param("versionStatus") com.memories.platform.template.entity.TemplateVersionStatus versionStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select template from Template template where template.id = :id")
    Optional<Template> findForUpdateById(@Param("id") UUID id);
}
