package com.memories.platform.memory.dto;

import com.memories.platform.memory.entity.MemoryCollaboratorPermission;

public record MemoryCapabilitiesResponse(
        boolean owner,
        MemoryCollaboratorPermission collaboratorPermission,
        boolean canEdit,
        boolean canPublish,
        boolean canManageCollaborators,
        boolean canChangeAccessPolicy,
        boolean canManageGuests,
        boolean canArchive,
        boolean canDelete
) {
}
