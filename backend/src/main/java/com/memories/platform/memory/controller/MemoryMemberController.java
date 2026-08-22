package com.memories.platform.memory.controller;

import com.memories.platform.memory.dto.CreateMemoryMemberRequest;
import com.memories.platform.memory.dto.MemoryMemberResponse;
import com.memories.platform.memory.dto.ReorderMemoryItemsRequest;
import com.memories.platform.memory.dto.UpdateMemoryMemberRequest;
import com.memories.platform.memory.dto.UpdateMemoryAssetReferenceRequest;
import com.memories.platform.memory.service.MemoryMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories/{memoryId}/members")
public class MemoryMemberController {

    private final MemoryMemberService memberService;

    public MemoryMemberController(MemoryMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<MemoryMemberResponse>> list(@PathVariable UUID memoryId) {
        return ResponseEntity.ok(memberService.list(memoryId));
    }

    @PostMapping
    public ResponseEntity<MemoryMemberResponse> create(
            @PathVariable UUID memoryId,
            @Valid @RequestBody CreateMemoryMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                memberService.create(memoryId, request)
        );
    }

    @PutMapping("/order")
    public ResponseEntity<List<MemoryMemberResponse>> reorder(
            @PathVariable UUID memoryId,
            @Valid @RequestBody ReorderMemoryItemsRequest request
    ) {
        return ResponseEntity.ok(memberService.reorder(memoryId, request));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<MemoryMemberResponse> update(
            @PathVariable UUID memoryId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemoryMemberRequest request
    ) {
        return ResponseEntity.ok(memberService.update(memoryId, memberId, request));
    }

    @PutMapping("/{memberId}/avatar")
    public ResponseEntity<MemoryMemberResponse> updateAvatar(
            @PathVariable UUID memoryId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemoryAssetReferenceRequest request
    ) {
        return ResponseEntity.ok(memberService.updateAvatar(memoryId, memberId, request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID memoryId,
            @PathVariable UUID memberId,
            @RequestParam long version
    ) {
        memberService.delete(memoryId, memberId, version);
        return ResponseEntity.noContent().build();
    }
}
