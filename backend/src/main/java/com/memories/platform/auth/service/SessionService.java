package com.memories.platform.auth.service;

import com.memories.platform.auth.constants.AuthConstants;
import com.memories.platform.auth.dto.LoginResponse;
import com.memories.platform.auth.exception.InvalidRefreshTokenException;
import com.memories.platform.auth.service.AccessTokenService.AccessToken;
import com.memories.platform.auth.service.RefreshTokenPersistenceService.RotationResult;
import com.memories.platform.auth.service.RefreshTokenPersistenceService.StoredRefreshToken;
import com.memories.platform.utils.TokenHashUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class SessionService {

    private final RefreshTokenPersistenceService refreshTokenPersistenceService;
    private final AccessTokenService accessTokenService;
    private final CurrentActorService currentActorService;
    private final SecureRandom secureRandom;

    public SessionService(
            RefreshTokenPersistenceService refreshTokenPersistenceService,
            AccessTokenService accessTokenService,
            CurrentActorService currentActorService,
            SecureRandom secureRandom
    ) {
        this.refreshTokenPersistenceService = refreshTokenPersistenceService;
        this.accessTokenService = accessTokenService;
        this.currentActorService = currentActorService;
        this.secureRandom = secureRandom;
    }

    public IssuedRefreshToken startSession(UUID userId) {
        String rawToken = generateRawToken();
        StoredRefreshToken storedToken = refreshTokenPersistenceService.issueInitial(
                userId,
                TokenHashUtils.sha256(rawToken)
        );
        return new IssuedRefreshToken(rawToken, storedToken.expiresAt());
    }

    public AuthenticatedSession refresh(String rawToken, String correlationId) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        String replacementRawToken = generateRawToken();
        RotationResult rotation = refreshTokenPersistenceService.rotate(
                TokenHashUtils.sha256(rawToken),
                TokenHashUtils.sha256(replacementRawToken),
                correlationId
        );
        if (rotation.outcome() != RefreshTokenPersistenceService.RotationOutcome.SUCCESS) {
            throw new InvalidRefreshTokenException();
        }

        AccessToken accessToken = accessTokenService.issue(rotation.userId());
        return new AuthenticatedSession(
                new LoginResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds()),
                new IssuedRefreshToken(replacementRawToken, rotation.expiresAt())
        );
    }

    public void logoutCurrent(String rawToken, String correlationId) {
        if (rawToken != null && !rawToken.isBlank()) {
            refreshTokenPersistenceService.revokeCurrent(TokenHashUtils.sha256(rawToken), correlationId);
        }
    }

    public void logoutAll(String correlationId) {
        refreshTokenPersistenceService.revokeAll(currentActorService.userId(), correlationId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[AuthConstants.REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {
    }

    public record AuthenticatedSession(
            LoginResponse response,
            IssuedRefreshToken refreshToken
    ) {
    }
}
