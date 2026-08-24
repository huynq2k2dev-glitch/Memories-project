package com.memories.platform.guest.service;

import com.memories.platform.guest.dto.GuestMessagePublicResponse;
import com.memories.platform.guest.entity.GuestMessageStatus;
import com.memories.platform.guest.repository.GuestMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GuestMessageQueryService {

    private final GuestMessageRepository messageRepository;

    public GuestMessageQueryService(GuestMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional(readOnly = true)
    public List<GuestMessagePublicResponse> approved(UUID memoryId) {
        return messageRepository.findAllByMemoryIdAndStatusOrderByCreatedAtAsc(
                memoryId,
                GuestMessageStatus.APPROVED
        ).stream().map(message -> new GuestMessagePublicResponse(
                message.getId(),
                message.getGuestName(),
                message.getContent(),
                message.getCreatedAt()
        )).toList();
    }
}
