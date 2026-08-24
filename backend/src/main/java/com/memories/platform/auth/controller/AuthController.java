package com.memories.platform.auth.controller;

import com.memories.platform.auth.dto.ConfirmEmailVerificationRequest;
import com.memories.platform.auth.dto.EmailVerificationResponse;
import com.memories.platform.auth.dto.EmailVerificationStatusResponse;
import com.memories.platform.auth.dto.LoginRequest;
import com.memories.platform.auth.dto.LoginResponse;
import com.memories.platform.auth.dto.RegistrationRequest;
import com.memories.platform.auth.dto.RegistrationResponse;
import com.memories.platform.auth.dto.ResendEmailVerificationRequest;
import com.memories.platform.auth.service.EmailVerificationService;
import com.memories.platform.auth.service.LoginService;
import com.memories.platform.auth.service.RefreshTokenCookieService;
import com.memories.platform.auth.service.RegistrationService;
import com.memories.platform.auth.service.RegistrationService.RegistrationResult;
import com.memories.platform.auth.service.SessionService;
import com.memories.platform.auth.service.SessionService.AuthenticatedSession;
import com.memories.platform.common.web.CorrelationIdFilter;
import com.memories.platform.ratelimit.service.ClientIpHashService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final LoginService loginService;
    private final SessionService sessionService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final ClientIpHashService clientIpHashService;

    public AuthController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            LoginService loginService,
            SessionService sessionService,
            RefreshTokenCookieService refreshTokenCookieService,
            ClientIpHashService clientIpHashService
    ) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.loginService = loginService;
        this.sessionService = sessionService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.clientIpHashService = clientIpHashService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request
    ) {
        RegistrationResult result = registrationService.register(
                request.email(),
                request.password(),
                request.displayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(
                result.id(),
                result.email(),
                result.status()
        ));
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<EmailVerificationResponse> confirmEmailVerification(
            @Valid @RequestBody ConfirmEmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.confirm(request.token()));
    }

    @PostMapping("/email-verifications/resend")
    public ResponseEntity<EmailVerificationStatusResponse> resendEmailVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request
    ) {
        return ResponseEntity.accepted().body(
                emailVerificationService.resend(request.email())
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        AuthenticatedSession session = loginService.login(
                request.email(),
                request.password(),
                correlationId,
                clientIpHashService.hash(httpRequest)
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.create(
                                session.refreshToken().rawToken(),
                                session.refreshToken().expiresAt()
                        )
                )
                .body(session.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "${platform.auth.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        AuthenticatedSession session = sessionService.refresh(refreshToken, correlationId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.create(
                                session.refreshToken().rawToken(),
                                session.refreshToken().expiresAt()
                        )
                )
                .body(session.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "${platform.auth.refresh-cookie-name}", required = false) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        String correlationId = (String) httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        sessionService.logoutCurrent(refreshToken, correlationId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear())
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(HttpServletRequest httpRequest) {
        String correlationId = (String) httpRequest.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        sessionService.logoutAll(correlationId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear())
                .build();
    }
}
