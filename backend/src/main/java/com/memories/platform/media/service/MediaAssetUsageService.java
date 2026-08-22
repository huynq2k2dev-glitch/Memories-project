package com.memories.platform.media.service;

import java.util.UUID;

public interface MediaAssetUsageService {

    boolean isInUse(UUID assetId);
}
