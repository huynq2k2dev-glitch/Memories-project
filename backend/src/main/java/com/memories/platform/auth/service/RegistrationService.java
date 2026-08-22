package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.VerificationEmail;
import com.memories.platform.auth.exception.EmailAlreadyRegisteredException;
import com.memories.platform.auth.exception.VerificationEmailDeliveryException;
import com.memories.platform.auth.repository.UserAccountRepository;
import com.memories.platform.auth.service.RegistrationPersistenceService.PendingRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {

    private final RegistrationPersistenceService persistenceService;
    private final UserAccountRepository userRepository;
    private final VerificationEmailSender emailSender;
    private final String verificationUrlBase;

    public RegistrationService(
            RegistrationPersistenceService persistenceService,
            UserAccountRepository userRepository,
            VerificationEmailSender emailSender,
            @Value("${platform.auth.verification-url-base}") String verificationUrlBase
    ) {
        this.persistenceService = persistenceService;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.verificationUrlBase = verificationUrlBase;
    }

    public RegistrationResult register(String rawEmail, String password, String rawDisplayName) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        PendingRegistration pendingRegistration;
        try {
            pendingRegistration = persistenceService.create(
                    email,
                    password,
                    rawDisplayName.trim()
            );
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }

        URI verificationUri = URI.create(
                verificationUrlBase + "#token=" + pendingRegistration.rawToken()
        );
        try {
            emailSender.send(new VerificationEmail(
                    pendingRegistration.email(),
                    verificationUri,
                    pendingRegistration.expiresAt()
            ));
        } catch (RuntimeException exception) {
            throw new VerificationEmailDeliveryException(exception);
        }

        return new RegistrationResult(
                pendingRegistration.userId(),
                pendingRegistration.email(),
                pendingRegistration.status().name()
        );
    }

    public record RegistrationResult(UUID id, String email, String status) {
    }
}
