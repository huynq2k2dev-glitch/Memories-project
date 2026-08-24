package com.memories.platform.memory.service;

import com.memories.platform.auth.service.CurrentActorService;
import com.memories.platform.guest.service.GuestMessageQueryService;
import com.memories.platform.media.dto.MediaDeliveryResponse;
import com.memories.platform.media.dto.ReadyMediaAsset;
import com.memories.platform.media.dto.ReadyMediaAssetMetadata;
import com.memories.platform.media.service.MediaAssetAccessService;
import com.memories.platform.media.service.MediaDeliveryService;
import com.memories.platform.memory.constants.MemoryPublishingConstants;
import com.memories.platform.memory.dto.MemoryRenderResponse;
import com.memories.platform.memory.entity.Memory;
import com.memories.platform.memory.entity.MemoryImage;
import com.memories.platform.memory.entity.MemoryMember;
import com.memories.platform.memory.entity.MemorySection;
import com.memories.platform.memory.entity.MemoryStatus;
import com.memories.platform.memory.entity.MemoryVisibility;
import com.memories.platform.memory.exception.MemoryPasswordRequiredException;
import com.memories.platform.memory.exception.MemoryMediaNotFoundException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.repository.MemoryEventRepository;
import com.memories.platform.memory.repository.MemoryImageRepository;
import com.memories.platform.memory.repository.MemoryLocationRepository;
import com.memories.platform.memory.repository.MemoryMemberRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import com.memories.platform.memory.repository.MemorySectionRepository;
import com.memories.platform.template.dto.TemplateRenderContractResponse;
import com.memories.platform.template.service.TemplateRenderContractService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemoryRenderService {

    private static final String PUBLIC_MEDIA_PATH = "/api/v1/public/media/";

    private final MemoryRepository memoryRepository;
    private final MemoryMemberRepository memberRepository;
    private final MemorySectionRepository sectionRepository;
    private final MemoryLocationRepository locationRepository;
    private final MemoryEventRepository eventRepository;
    private final MemoryImageRepository imageRepository;
    private final GuestMessageQueryService guestMessageQueryService;
    private final MemoryAccessService accessService;
    private final MemoryPasswordAccessService passwordAccessService;
    private final MemoryShareLinkService shareLinkService;
    private final CurrentActorService currentActorService;
    private final TemplateRenderContractService renderContractService;
    private final MediaAssetAccessService assetAccessService;
    private final MediaDeliveryService mediaDeliveryService;
    private final Clock clock;

    public MemoryRenderService(
            MemoryRepository memoryRepository,
            MemoryMemberRepository memberRepository,
            MemorySectionRepository sectionRepository,
            MemoryLocationRepository locationRepository,
            MemoryEventRepository eventRepository,
            MemoryImageRepository imageRepository,
            GuestMessageQueryService guestMessageQueryService,
            MemoryAccessService accessService,
            MemoryPasswordAccessService passwordAccessService,
            MemoryShareLinkService shareLinkService,
            CurrentActorService currentActorService,
            TemplateRenderContractService renderContractService,
            MediaAssetAccessService assetAccessService,
            MediaDeliveryService mediaDeliveryService,
            Clock clock
    ) {
        this.memoryRepository = memoryRepository;
        this.memberRepository = memberRepository;
        this.sectionRepository = sectionRepository;
        this.locationRepository = locationRepository;
        this.eventRepository = eventRepository;
        this.imageRepository = imageRepository;
        this.guestMessageQueryService = guestMessageQueryService;
        this.accessService = accessService;
        this.passwordAccessService = passwordAccessService;
        this.shareLinkService = shareLinkService;
        this.currentActorService = currentActorService;
        this.renderContractService = renderContractService;
        this.assetAccessService = assetAccessService;
        this.mediaDeliveryService = mediaDeliveryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MemoryRenderResponse preview(UUID memoryId) {
        Memory memory = accessService.requireView(memoryId);
        return render(memory, false);
    }

    @Transactional(readOnly = true)
    public MemoryRenderResponse publicMemory(String slug, Map<String, String> cookies) {
        Memory memory = memoryRepository.findPublicBySlug(
                slug,
                MemoryStatus.PUBLISHED,
                MemoryPublishingConstants.PUBLISHABLE_VISIBILITIES,
                clock.instant()
        ).orElseThrow(MemoryNotFoundException::new);
        return switch (memory.getVisibility()) {
            case PUBLIC, UNLISTED -> render(memory, true);
            case PRIVATE -> {
                if (accessService.canView(memory, currentActorService.userIdOrNull())) {
                    yield render(memory, false);
                }
                if (!shareLinkService.hasValidViewGrant(memory.getId(), cookies)) {
                    throw new MemoryNotFoundException();
                }
                yield render(memory, true);
            }
            case PASSWORD_PROTECTED -> {
                if (accessService.canView(memory, currentActorService.userIdOrNull())) {
                    yield render(memory, false);
                }
                if (!passwordAccessService.hasValidGrant(memory.getId(), cookies)) {
                    throw new MemoryPasswordRequiredException();
                }
                yield render(memory, true);
            }
        };
    }

    @Transactional(readOnly = true)
    public MediaDeliveryResponse publicDelivery(
            UUID assetId,
            Map<String, String> cookies
    ) {
        boolean publiclyReferenced = memoryRepository.isAssetPubliclyReferenced(
                assetId,
                MemoryStatus.PUBLISHED,
                MemoryPublishingConstants.PUBLIC_VISIBILITIES,
                clock.instant()
        );
        boolean passwordAccess = !publiclyReferenced
                && memoryRepository.findMemoryIdsReferencingAsset(
                        assetId,
                        MemoryStatus.PUBLISHED,
                        MemoryVisibility.PASSWORD_PROTECTED,
                        clock.instant()
                ).stream().anyMatch(memoryId -> passwordAccessService.hasValidGrant(
                        memoryId,
                        cookies
                ));
        boolean shareAccess = !publiclyReferenced
                && !passwordAccess
                && memoryRepository.findMemoryIdsReferencingAsset(
                        assetId,
                        MemoryStatus.PUBLISHED,
                        MemoryVisibility.PRIVATE,
                        clock.instant()
                ).stream().anyMatch(memoryId -> shareLinkService.hasValidViewGrant(
                        memoryId,
                        cookies
                ));
        if (!publiclyReferenced && !passwordAccess && !shareAccess) {
            throw new MemoryMediaNotFoundException();
        }
        return mediaDeliveryService.delivery(assetId);
    }

    private MemoryRenderResponse render(Memory memory, boolean publicView) {
        TemplateRenderContractResponse contract = renderContractService.requireRenderable(
                memory.getTemplateVersionId()
        );
        List<MemoryMember> members = memberRepository.findAllByMemoryIdOrderBySortOrderAsc(
                memory.getId()
        );
        List<MemorySection> sections = sectionRepository.findAllByMemoryIdOrderBySortOrderAsc(
                memory.getId()
        ).stream().filter(MemorySection::isVisible).toList();
        Set<UUID> visibleSectionIds = sections.stream()
                .map(MemorySection::getId)
                .collect(Collectors.toUnmodifiableSet());
        List<MemoryImage> images = imageRepository.findAllByMemoryIdOrderBySortOrderAsc(
                memory.getId()
        ).stream().filter(image -> image.getSectionId() == null
                || visibleSectionIds.contains(image.getSectionId())).toList();

        Set<UUID> assetIds = assetIds(memory, members, images);
        Map<UUID, RenderAsset> assets = publicView
                ? publicAssets(assetIds)
                : previewAssets(assetIds);

        return new MemoryRenderResponse(
                memory.getSlug(),
                memory.getTitle(),
                memory.getMemoryType(),
                memory.getStatus(),
                memory.getVisibility(),
                memory.getSummary(),
                memory.getThemeConfig().deepCopy(),
                memory.getEventStartAt(),
                memory.getPublishedAt(),
                memory.getExpiresAt(),
                contract.templateVersionId(),
                contract.componentKey(),
                contract.rendererVersion(),
                media(memory.getCoverAssetId(), assets),
                members.stream().map(member -> new MemoryRenderResponse.RenderMember(
                        member.getId(),
                        member.getRoleCode(),
                        member.getFullName(),
                        member.getDisplayName(),
                        member.getDescription(),
                        media(member.getAvatarAssetId(), assets),
                        member.getSortOrder()
                )).toList(),
                sections.stream().map(section -> new MemoryRenderResponse.RenderSection(
                        section.getId(),
                        section.getSectionKey(),
                        section.getSectionType(),
                        section.getTitle(),
                        section.getContentText(),
                        section.getConfig().deepCopy(),
                        section.getSortOrder(),
                        contract.requiredSectionTypes().contains(section.getSectionType()),
                        section.hasContent()
                )).toList(),
                locations(memory.getId()),
                events(memory.getId()),
                images.stream().map(image -> new MemoryRenderResponse.RenderImage(
                        image.getId(),
                        image.getSectionId(),
                        image.getCaption(),
                        image.getAltText(),
                        image.getSortOrder(),
                        image.isCoverCandidate(),
                        media(image.getMediaAssetId(), assets)
                )).toList(),
                guestMessageQueryService.approved(memory.getId())
        );
    }

    private List<MemoryRenderResponse.RenderLocation> locations(UUID memoryId) {
        return locationRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(location -> new MemoryRenderResponse.RenderLocation(
                        location.getId(),
                        location.getName(),
                        location.getAddress(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getMapUrl(),
                        location.getSortOrder()
                )).toList();
    }

    private List<MemoryRenderResponse.RenderEvent> events(UUID memoryId) {
        return eventRepository.findAllByMemoryIdOrderBySortOrderAsc(memoryId).stream()
                .map(event -> new MemoryRenderResponse.RenderEvent(
                        event.getId(),
                        event.getLocationId(),
                        event.getEventType(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartAt(),
                        event.getEndAt(),
                        event.getTimezone(),
                        event.getSortOrder(),
                        event.isRsvpEnabled()
                )).toList();
    }

    private Set<UUID> assetIds(
            Memory memory,
            List<MemoryMember> members,
            List<MemoryImage> images
    ) {
        Set<UUID> assetIds = new HashSet<>();
        if (memory.getCoverAssetId() != null) {
            assetIds.add(memory.getCoverAssetId());
        }
        members.stream()
                .map(MemoryMember::getAvatarAssetId)
                .filter(Objects::nonNull)
                .forEach(assetIds::add);
        images.stream().map(MemoryImage::getMediaAssetId).forEach(assetIds::add);
        return Set.copyOf(assetIds);
    }

    private Map<UUID, RenderAsset> previewAssets(Set<UUID> assetIds) {
        return assetAccessService.ready(assetIds).values().stream()
                .map(this::toRenderAsset)
                .collect(Collectors.toUnmodifiableMap(RenderAsset::id, Function.identity()));
    }

    private Map<UUID, RenderAsset> publicAssets(Set<UUID> assetIds) {
        return assetAccessService.readyMetadata(assetIds).values().stream()
                .map(this::toRenderAsset)
                .collect(Collectors.toUnmodifiableMap(RenderAsset::id, Function.identity()));
    }

    private RenderAsset toRenderAsset(ReadyMediaAsset asset) {
        return new RenderAsset(
                asset.id(),
                asset.mimeType(),
                asset.fileSize(),
                asset.deliveryUrl()
        );
    }

    private RenderAsset toRenderAsset(ReadyMediaAssetMetadata asset) {
        return new RenderAsset(
                asset.id(),
                asset.mimeType(),
                asset.fileSize(),
                PUBLIC_MEDIA_PATH + asset.id()
        );
    }

    private MemoryRenderResponse.RenderMedia media(
            UUID assetId,
            Map<UUID, RenderAsset> assets
    ) {
        if (assetId == null) {
            return null;
        }
        RenderAsset asset = assets.get(assetId);
        if (asset == null) {
            throw new MemoryMediaNotFoundException();
        }
        return new MemoryRenderResponse.RenderMedia(
                asset.id(),
                asset.mimeType(),
                asset.fileSize(),
                asset.deliveryUrl()
        );
    }

    private record RenderAsset(
            UUID id,
            String mimeType,
            long fileSize,
            String deliveryUrl
    ) {
    }
}
