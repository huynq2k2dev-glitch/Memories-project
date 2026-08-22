package com.memories.platform.memory.service;

import com.memories.platform.media.service.MediaAssetUsageService;
import com.memories.platform.memory.repository.MemoryImageRepository;
import com.memories.platform.memory.repository.MemoryMemberRepository;
import com.memories.platform.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MemoryMediaAssetUsageService implements MediaAssetUsageService {

    private final MemoryImageRepository imageRepository;
    private final MemoryRepository memoryRepository;
    private final MemoryMemberRepository memberRepository;

    public MemoryMediaAssetUsageService(
            MemoryImageRepository imageRepository,
            MemoryRepository memoryRepository,
            MemoryMemberRepository memberRepository
    ) {
        this.imageRepository = imageRepository;
        this.memoryRepository = memoryRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean isInUse(UUID assetId) {
        return imageRepository.existsByMediaAssetId(assetId)
                || memoryRepository.existsByCoverAssetId(assetId)
                || memberRepository.existsByAvatarAssetId(assetId);
    }
}
