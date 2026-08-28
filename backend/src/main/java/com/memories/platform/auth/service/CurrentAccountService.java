package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.CurrentAccountResponse;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.exception.UserAccountNotFoundException;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentAccountService {

    private final CurrentActorService currentActorService;
    private final UserAccountRepository userAccountRepository;

    public CurrentAccountService(
            CurrentActorService currentActorService,
            UserAccountRepository userAccountRepository
    ) {
        this.currentActorService = currentActorService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public CurrentAccountResponse get() {
        UserAccount account = userAccountRepository.findById(currentActorService.userId())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(UserAccountNotFoundException::new);
        return new CurrentAccountResponse(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getStatus()
        );
    }
}
