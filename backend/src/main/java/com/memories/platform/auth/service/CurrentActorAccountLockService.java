package com.memories.platform.auth.service;

import com.memories.platform.auth.exception.UserAccountNotFoundException;
import com.memories.platform.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CurrentActorAccountLockService {

    private final CurrentActorService currentActorService;
    private final UserAccountRepository userAccountRepository;

    public CurrentActorAccountLockService(
            CurrentActorService currentActorService,
            UserAccountRepository userAccountRepository
    ) {
        this.currentActorService = currentActorService;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID lockCurrentAccount() {
        UUID actorId = currentActorService.userId();
        userAccountRepository.findForUpdateById(actorId)
                .orElseThrow(UserAccountNotFoundException::new);
        return actorId;
    }
}
