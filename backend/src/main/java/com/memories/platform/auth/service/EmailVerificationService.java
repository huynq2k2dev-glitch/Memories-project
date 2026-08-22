package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.EmailVerificationResponse;
import com.memories.platform.auth.dto.EmailVerificationStatusResponse;
import com.memories.platform.auth.dto.VerificationEmail;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.entity.VerificationToken;
import com.memories.platform.auth.entity.VerificationTokenType;
import com.memories.platform.auth.exception.ExpiredVerificationTokenException;
import com.memories.platform.auth.exception.InvalidVerificationTokenException;
import com.memories.platform.auth.exception.VerificationEmailDeliveryException;
import com.memories.platform.auth.repository.VerificationTokenRepository;
import com.memories.platform.auth.service.RegistrationPersistenceService.PendingVerificationEmail;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;


@Service
public class EmailVerificationService {

    private final VerificationTokenRepository tokenRepository;
    private final RegistrationPersistenceService persistenceService;
    private final VerificationEmailSender emailSender;
    private final Clock clock;
    private final String verificationUrlBase;

    public EmailVerificationService(
            VerificationTokenRepository tokenRepository,
            RegistrationPersistenceService persistenceService,
            VerificationEmailSender emailSender,
            Clock clock,
            @Value("${platform.auth.verification-url-base}") String verificationUrlBase
    ) {
        this.tokenRepository = tokenRepository;
        this.persistenceService = persistenceService;
        this.emailSender = emailSender;
        this.clock = clock;
        this.verificationUrlBase = verificationUrlBase;
    }

    @Transactional
    public EmailVerificationResponse confirm(String rawToken) {
        VerificationToken token = tokenRepository.findForUpdateByTokenHashAndType(
                        TokenHashUtils.sha256(rawToken),
                        VerificationTokenType.EMAIL_VERIFY
                )
                .orElseThrow(InvalidVerificationTokenException::new);

        Instant now = clock.instant();
        UserAccount user = token.getUser();
        if (token.isUsed() || !user.canVerifyEmail(token.getTarget())) {
            throw new InvalidVerificationTokenException();
        }
        if (token.isExpiredAt(now)) {
            throw new ExpiredVerificationTokenException();
        }

        user.verifyEmail(now);
        token.markUsed(now);
        return new EmailVerificationResponse("VERIFIED", "/login");
    }

    public EmailVerificationStatusResponse resend(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Optional<PendingVerificationEmail> pending =
                persistenceService.renewEmailVerification(email);
        pending.ifPresent(this::sendVerificationEmail);
        return new EmailVerificationStatusResponse("ACCEPTED");
    }

    private void sendVerificationEmail(PendingVerificationEmail pending) {
        URI verificationUri = URI.create(
                verificationUrlBase + "#token=" + pending.rawToken()
        );
        try {
            emailSender.send(new VerificationEmail(
                    pending.email(),
                    verificationUri,
                    pending.expiresAt()
            ));
        } catch (RuntimeException exception) {
            throw new VerificationEmailDeliveryException(exception);
        }
    }
}
