package com.memories.platform.auth.service;

import com.memories.platform.auth.dto.LoginResponse;
import com.memories.platform.auth.exception.AccountLockedException;
import com.memories.platform.auth.exception.EmailNotVerifiedException;
import com.memories.platform.auth.exception.InvalidCredentialsException;
import com.memories.platform.auth.exception.LoginRateLimitException;
import com.memories.platform.auth.service.AccessTokenService.AccessToken;
import com.memories.platform.auth.service.LoginPersistenceService.LoginAttempt;
import com.memories.platform.auth.service.SessionService.AuthenticatedSession;
import com.memories.platform.auth.service.SessionService.IssuedRefreshToken;
import com.memories.platform.config.RateLimitProperties;
import com.memories.platform.ratelimit.constants.RateLimitScope;
import com.memories.platform.ratelimit.service.RateLimitService;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LoginService {

    private final LoginPersistenceService loginPersistenceService;
    private final AccessTokenService accessTokenService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    public LoginService(
            LoginPersistenceService loginPersistenceService,
            AccessTokenService accessTokenService,
            SessionService sessionService,
            RateLimitService rateLimitService,
            RateLimitProperties rateLimitProperties
    ) {
        this.loginPersistenceService = loginPersistenceService;
        this.accessTokenService = accessTokenService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
        this.rateLimitProperties = rateLimitProperties;
    }

    public AuthenticatedSession login(
            String email,
            String password,
            String correlationId,
            String clientIpHash
    ) {
        if (!rateLimitService.tryAcquire(
                RateLimitScope.LOGIN,
                clientIpHash,
                rateLimitProperties.loginCount(),
                rateLimitProperties.loginWindow()
        )) {
            loginPersistenceService.recordRateLimited(correlationId);
            throw new LoginRateLimitException();
        }
        LoginAttempt attempt = loginPersistenceService.authenticate(
                email.trim().toLowerCase(Locale.ROOT),
                password,
                correlationId
        );
        switch (attempt.outcome()) {
            case INVALID_CREDENTIALS -> throw new InvalidCredentialsException();
            case EMAIL_NOT_VERIFIED -> throw new EmailNotVerifiedException();
            case ACCOUNT_LOCKED -> throw new AccountLockedException();
            case SUCCESS -> {
                AccessToken accessToken = accessTokenService.issue(attempt.userId());
                IssuedRefreshToken refreshToken = sessionService.startSession(attempt.userId());
                return new AuthenticatedSession(
                        new LoginResponse(accessToken.value(), "Bearer", accessToken.expiresInSeconds()),
                        refreshToken
                );
            }
        }
        throw new IllegalStateException("Unsupported login outcome");
    }
}
