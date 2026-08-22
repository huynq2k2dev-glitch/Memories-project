package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.entity.Role;
import com.memories.platform.auth.entity.UserAccount;
import com.memories.platform.auth.entity.UserRole;
import com.memories.platform.auth.entity.UserStatus;
import com.memories.platform.auth.entity.VerificationToken;
import com.memories.platform.auth.entity.VerificationTokenType;
import com.memories.platform.auth.exception.EmailAlreadyRegisteredException;
import com.memories.platform.auth.repository.RoleRepository;
import com.memories.platform.auth.repository.UserAccountRepository;
import com.memories.platform.auth.repository.UserRoleRepository;
import com.memories.platform.auth.repository.VerificationTokenRepository;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RegistrationPersistenceService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Duration tokenTtl;

    public RegistrationPersistenceService(
            UserAccountRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            VerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            SecureRandom secureRandom,
            Clock clock,
            @Value("${platform.auth.verification-token-ttl}") Duration tokenTtl
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
    }

    @Transactional
    public PendingRegistration create(String email, String password, String displayName) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        Instant now = clock.instant();
        UserAccount user = new UserAccount(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(password),
                displayName,
                now
        );
        userRepository.save(user);

        Role defaultRole = roleRepository.findByCode(AuthConstants.DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing"));
        userRoleRepository.save(new UserRole(user, defaultRole, now));

        String rawToken = generateToken();
        Instant expiresAt = now.plus(tokenTtl);
        tokenRepository.save(new VerificationToken(
                UUID.randomUUID(),
                user,
                TokenHashUtils.sha256(rawToken),
                VerificationTokenType.EMAIL_VERIFY,
                email,
                expiresAt,
                now
        ));
        userRepository.flush();

        return new PendingRegistration(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                rawToken,
                expiresAt
        );
    }

    @Transactional
    public Optional<PendingVerificationEmail> renewEmailVerification(String email) {
        Optional<UserAccount> existingUser = userRepository.findForUpdateByEmail(email);
        if (existingUser.isEmpty()
                || existingUser.get().getStatus() != UserStatus.PENDING_VERIFICATION) {
            return Optional.empty();
        }

        UserAccount user = existingUser.get();
        Instant now = clock.instant();
        tokenRepository.findAllByUserAndTypeAndUsedAtIsNull(
                user,
                VerificationTokenType.EMAIL_VERIFY
        ).forEach(token -> token.markUsed(now));

        String rawToken = generateToken();
        Instant expiresAt = now.plus(tokenTtl);
        tokenRepository.saveAndFlush(new VerificationToken(
                UUID.randomUUID(),
                user,
                TokenHashUtils.sha256(rawToken),
                VerificationTokenType.EMAIL_VERIFY,
                email,
                expiresAt,
                now
        ));
        return Optional.of(new PendingVerificationEmail(email, rawToken, expiresAt));
    }

    private String generateToken() {
        byte[] bytes = new byte[AuthConstants.VERIFICATION_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record PendingRegistration(
            UUID userId,
            String email,
            UserStatus status,
            String rawToken,
            Instant expiresAt
    ) {
    }

    public record PendingVerificationEmail(
            String email,
            String rawToken,
            Instant expiresAt
    ) {
    }
}
