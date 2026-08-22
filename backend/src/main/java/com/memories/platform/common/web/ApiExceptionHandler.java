package com.memories.platform.common.web;

import com.memories.platform.auth.exception.AccountLockedException;
import com.memories.platform.auth.exception.EmailAlreadyRegisteredException;
import com.memories.platform.auth.exception.EmailNotVerifiedException;
import com.memories.platform.auth.exception.ExpiredVerificationTokenException;
import com.memories.platform.auth.exception.InvalidVerificationTokenException;
import com.memories.platform.auth.exception.InvalidCredentialsException;
import com.memories.platform.auth.exception.InvalidRefreshTokenException;
import com.memories.platform.auth.exception.PermissionDeniedException;
import com.memories.platform.auth.exception.UserAccountNotFoundException;
import com.memories.platform.auth.service.RefreshTokenCookieService;
import com.memories.platform.auth.exception.VerificationEmailDeliveryException;
import com.memories.platform.media.exception.InvalidMediaUploadException;
import com.memories.platform.media.exception.MediaAssetInUseException;
import com.memories.platform.media.exception.MediaAssetNotFoundException;
import com.memories.platform.media.exception.MediaAssetNotReadyException;
import com.memories.platform.media.exception.MediaQuotaExceededException;
import com.memories.platform.media.exception.MediaStorageUnavailableException;
import com.memories.platform.media.exception.MediaUploadVerificationException;
import com.memories.platform.media.exception.MediaVersionConflictException;
import com.memories.platform.memory.exception.MemoryNotFoundException;
import com.memories.platform.memory.exception.MemoryMediaNotFoundException;
import com.memories.platform.memory.exception.MemorySlugConflictException;
import com.memories.platform.memory.exception.MemoryTemplateTypeMismatchException;
import com.memories.platform.memory.exception.InvalidMemoryThemeException;
import com.memories.platform.memory.exception.MemoryNotEditableException;
import com.memories.platform.memory.exception.MemoryPublishValidationException;
import com.memories.platform.memory.exception.MemoryVersionConflictException;
import com.memories.platform.memory.exception.UnsafeMemoryContentException;
import com.memories.platform.memory.exception.UnsupportedMemoryVisibilityException;
import com.memories.platform.memory.exception.InvalidMemoryItemOrderException;
import com.memories.platform.memory.exception.InvalidMemorySectionContractException;
import com.memories.platform.memory.exception.MemoryMemberConflictException;
import com.memories.platform.memory.exception.MemoryMemberNotFoundException;
import com.memories.platform.memory.exception.MemorySectionConflictException;
import com.memories.platform.memory.exception.MemorySectionNotFoundException;
import com.memories.platform.memory.exception.InvalidMemoryCoordinatesException;
import com.memories.platform.memory.exception.InvalidMemoryEventTimeException;
import com.memories.platform.memory.exception.InvalidMemoryLocationReferenceException;
import com.memories.platform.memory.exception.InvalidMemoryTimezoneException;
import com.memories.platform.memory.exception.MemoryEventConflictException;
import com.memories.platform.memory.exception.MemoryEventNotFoundException;
import com.memories.platform.memory.exception.MemoryLocationConflictException;
import com.memories.platform.memory.exception.MemoryLocationNotFoundException;
import com.memories.platform.memory.exception.UnsafeMemoryMapUrlException;
import com.memories.platform.memory.exception.InvalidMemorySectionReferenceException;
import com.memories.platform.memory.exception.MemoryImageConflictException;
import com.memories.platform.memory.exception.MemoryImageNotFoundException;
import com.memories.platform.template.exception.InvalidTemplateContractException;
import com.memories.platform.template.exception.InvalidTemplateCatalogQueryException;
import com.memories.platform.template.exception.TemplateCodeAlreadyExistsException;
import com.memories.platform.template.exception.TemplateNotFoundException;
import com.memories.platform.template.exception.TemplateVersionImmutableException;
import com.memories.platform.template.exception.TemplateVersionNotFoundException;
import com.memories.platform.template.exception.TemplateVersionNotSelectableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final RefreshTokenCookieService refreshTokenCookieService;

    public ApiExceptionHandler(RefreshTokenCookieService refreshTokenCookieService) {
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        ResponseEntity<ProblemDetail> response = problem(
                HttpStatus.UNAUTHORIZED,
                "SESSION_INVALID",
                "The session is invalid or has expired.",
                request
        );
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear())
                .body(response.getBody());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "The email or password is incorrect.",
                request
        );
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ProblemDetail> handleEmailNotVerified(
            EmailNotVerifiedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "EMAIL_NOT_VERIFIED",
                "The account email has not been verified.",
                request
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(
            AccountLockedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.LOCKED,
                "ACCOUNT_LOCKED",
                "The account is temporarily locked. Please retry later.",
                request
        );
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ProblemDetail> handlePermissionDenied(
            PermissionDeniedException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this action.",
                request
        );
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserAccountNotFound(
            UserAccountNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "ACCOUNT_NOT_FOUND",
                "The account was not found.",
                request
        );
    }

    @ExceptionHandler(TemplateCodeAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleTemplateCodeAlreadyExists(
            TemplateCodeAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "TEMPLATE_CODE_ALREADY_EXISTS",
                "A template with this code already exists.",
                request
        );
    }

    @ExceptionHandler({TemplateNotFoundException.class, TemplateVersionNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleTemplateNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "TEMPLATE_NOT_FOUND",
                "The template or template version was not found.",
                request
        );
    }

    @ExceptionHandler(TemplateVersionImmutableException.class)
    public ResponseEntity<ProblemDetail> handleTemplateVersionImmutable(
            TemplateVersionImmutableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "TEMPLATE_VERSION_IMMUTABLE",
                "Only draft template versions can be changed or published.",
                request
        );
    }

    @ExceptionHandler(InvalidTemplateContractException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTemplateContract(
            InvalidTemplateContractException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getCode(),
                exception.getSafeDetail(),
                request
        );
    }

    @ExceptionHandler(InvalidTemplateCatalogQueryException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTemplateCatalogQuery(
            InvalidTemplateCatalogQueryException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "TEMPLATE_CATALOG_QUERY_INVALID",
                "Page must be non-negative and size must be between 1 and 50.",
                request
        );
    }

    @ExceptionHandler(TemplateVersionNotSelectableException.class)
    public ResponseEntity<ProblemDetail> handleTemplateVersionNotSelectable(
            TemplateVersionNotSelectableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "TEMPLATE_VERSION_NOT_SELECTABLE",
                "The selected template version is not available for new memories.",
                request
        );
    }

    @ExceptionHandler(MemoryTemplateTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMemoryTemplateTypeMismatch(
            MemoryTemplateTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_TEMPLATE_TYPE_MISMATCH",
                "The memory type is not compatible with the selected template.",
                request
        );
    }

    @ExceptionHandler(MemorySlugConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemorySlugConflict(
            MemorySlugConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_SLUG_CONFLICT",
                "A unique memory slug could not be generated. Please retry.",
                request
        );
    }

    @ExceptionHandler(MemoryNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryNotFound(
            MemoryNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_NOT_FOUND",
                "The memory was not found.",
                request
        );
    }

    @ExceptionHandler(MemoryMediaNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryMediaNotFound(
            MemoryMediaNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_MEDIA_NOT_FOUND",
                "The media is not available through a public memory.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryThemeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryTheme(
            InvalidMemoryThemeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_THEME_INVALID",
                "The theme config does not satisfy the selected template contract.",
                request
        );
    }

    @ExceptionHandler(UnsafeMemoryContentException.class)
    public ResponseEntity<ProblemDetail> handleUnsafeMemoryContent(
            UnsafeMemoryContentException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_CONTENT_UNSAFE",
                "Text fields may contain safe Markdown but not raw HTML or executable URLs.",
                request
        );
    }

    @ExceptionHandler(UnsupportedMemoryVisibilityException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMemoryVisibility(
            UnsupportedMemoryVisibilityException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_VISIBILITY_UNSUPPORTED",
                "Password-protected visibility is not available until a password is configured.",
                request
        );
    }

    @ExceptionHandler(MemoryNotEditableException.class)
    public ResponseEntity<ProblemDetail> handleMemoryNotEditable(
            MemoryNotEditableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_NOT_EDITABLE",
                "Only draft memories can be edited by this operation.",
                request
        );
    }

    @ExceptionHandler(MemoryPublishValidationException.class)
    public ResponseEntity<ProblemDetail> handleMemoryPublishValidation(
            MemoryPublishValidationException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getCode(),
                exception.getSafeDetail(),
                request
        );
    }

    @ExceptionHandler(MemoryVersionConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemoryVersionConflict(
            MemoryVersionConflictException exception,
            HttpServletRequest request
    ) {
        ResponseEntity<ProblemDetail> response = problem(
                HttpStatus.CONFLICT,
                "MEMORY_VERSION_CONFLICT",
                "The memory changed since it was loaded. Reload the current version before retrying.",
                request
        );
        if (exception.getCurrentVersion() != null && response.getBody() != null) {
            response.getBody().setProperty("currentVersion", exception.getCurrentVersion());
        }
        return response;
    }

    @ExceptionHandler(MemoryMemberNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryMemberNotFound(
            MemoryMemberNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_MEMBER_NOT_FOUND",
                "The memory member was not found.",
                request
        );
    }

    @ExceptionHandler(MemorySectionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemorySectionNotFound(
            MemorySectionNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_SECTION_NOT_FOUND",
                "The memory section was not found.",
                request
        );
    }

    @ExceptionHandler(MemoryMemberConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemoryMemberConflict(
            MemoryMemberConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_MEMBER_CONFLICT",
                "The member role and order conflict with an existing member.",
                request
        );
    }

    @ExceptionHandler(MemorySectionConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemorySectionConflict(
            MemorySectionConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_SECTION_CONFLICT",
                "The section key or order conflicts with an existing section.",
                request
        );
    }

    @ExceptionHandler(InvalidMemorySectionContractException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemorySectionContract(
            InvalidMemorySectionContractException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_SECTION_CONTRACT_INVALID",
                "The section type or config does not satisfy the pinned template contract.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryItemOrderException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryItemOrder(
            InvalidMemoryItemOrderException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "MEMORY_ITEM_ORDER_INVALID",
                "The ordered IDs and versions must match the current memory content.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryCoordinatesException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryCoordinates(
            InvalidMemoryCoordinatesException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_COORDINATES_INVALID",
                "Latitude and longitude must both be provided and remain in their valid ranges.",
                request
        );
    }

    @ExceptionHandler(UnsafeMemoryMapUrlException.class)
    public ResponseEntity<ProblemDetail> handleUnsafeMemoryMapUrl(
            UnsafeMemoryMapUrlException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_MAP_URL_UNSAFE",
                "Map URLs must use an approved HTTPS Google Maps or OpenStreetMap address.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryEventTimeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryEventTime(
            InvalidMemoryEventTimeException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_EVENT_TIME_INVALID",
                "The event end time cannot be earlier than its start time.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryTimezoneException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryTimezone(
            InvalidMemoryTimezoneException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_TIMEZONE_INVALID",
                "The timezone must be a valid IANA timezone identifier.",
                request
        );
    }

    @ExceptionHandler(InvalidMemoryLocationReferenceException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemoryLocationReference(
            InvalidMemoryLocationReferenceException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_LOCATION_REFERENCE_INVALID",
                "The selected location does not belong to this memory.",
                request
        );
    }

    @ExceptionHandler(MemoryLocationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryLocationNotFound(
            MemoryLocationNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_LOCATION_NOT_FOUND",
                "The memory location was not found.",
                request
        );
    }

    @ExceptionHandler(MemoryEventNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryEventNotFound(
            MemoryEventNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_EVENT_NOT_FOUND",
                "The memory event was not found.",
                request
        );
    }

    @ExceptionHandler(MemoryLocationConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemoryLocationConflict(
            MemoryLocationConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_LOCATION_CONFLICT",
                "The location could not be saved because its current data conflicts.",
                request
        );
    }

    @ExceptionHandler(MemoryEventConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemoryEventConflict(
            MemoryEventConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_EVENT_CONFLICT",
                "The event order conflicts with an existing event.",
                request
        );
    }

    @ExceptionHandler(InvalidMemorySectionReferenceException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMemorySectionReference(
            InvalidMemorySectionReferenceException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEMORY_SECTION_REFERENCE_INVALID",
                "The selected section does not belong to this memory.",
                request
        );
    }

    @ExceptionHandler(MemoryImageNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemoryImageNotFound(
            MemoryImageNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEMORY_IMAGE_NOT_FOUND",
                "The memory image was not found.",
                request
        );
    }

    @ExceptionHandler(MemoryImageConflictException.class)
    public ResponseEntity<ProblemDetail> handleMemoryImageConflict(
            MemoryImageConflictException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEMORY_IMAGE_CONFLICT",
                "The image asset or order conflicts with an existing memory image.",
                request
        );
    }

    @ExceptionHandler(InvalidMediaUploadException.class)
    public ResponseEntity<ProblemDetail> handleInvalidMediaUpload(
            InvalidMediaUploadException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEDIA_UPLOAD_INVALID",
                "Images must be JPEG, PNG, WebP, or AVIF and no larger than 10 MiB.",
                request
        );
    }

    @ExceptionHandler(MediaQuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleMediaQuotaExceeded(
            MediaQuotaExceededException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEDIA_QUOTA_EXCEEDED",
                "The media quota of 200 assets or 1 GiB has been reached.",
                request
        );
    }

    @ExceptionHandler(MediaAssetNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMediaAssetNotFound(
            MediaAssetNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "MEDIA_ASSET_NOT_FOUND",
                "The media asset was not found.",
                request
        );
    }

    @ExceptionHandler(MediaAssetNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleMediaAssetNotReady(
            MediaAssetNotReadyException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEDIA_ASSET_NOT_READY",
                "The media asset is not ready or does not belong to the current owner.",
                request
        );
    }

    @ExceptionHandler(MediaUploadVerificationException.class)
    public ResponseEntity<ProblemDetail> handleMediaUploadVerification(
            MediaUploadVerificationException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "MEDIA_UPLOAD_VERIFICATION_FAILED",
                "The uploaded object does not match its approved image metadata.",
                request
        );
    }

    @ExceptionHandler(MediaAssetInUseException.class)
    public ResponseEntity<ProblemDetail> handleMediaAssetInUse(
            MediaAssetInUseException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "MEDIA_ASSET_IN_USE",
                "Unlink this asset from all images, covers, and avatars before deleting it.",
                request
        );
    }

    @ExceptionHandler(MediaVersionConflictException.class)
    public ResponseEntity<ProblemDetail> handleMediaVersionConflict(
            MediaVersionConflictException exception,
            HttpServletRequest request
    ) {
        ResponseEntity<ProblemDetail> response = problem(
                HttpStatus.CONFLICT,
                "MEDIA_VERSION_CONFLICT",
                "The media asset changed since it was loaded.",
                request
        );
        if (exception.getCurrentVersion() != null && response.getBody() != null) {
            response.getBody().setProperty("currentVersion", exception.getCurrentVersion());
        }
        return response;
    }

    @ExceptionHandler(MediaStorageUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleMediaStorageUnavailable(
            MediaStorageUnavailableException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MEDIA_STORAGE_UNAVAILABLE",
                "Object storage is temporarily unavailable.",
                request
        );
    }

    @ExceptionHandler(ExpiredVerificationTokenException.class)
    public ResponseEntity<ProblemDetail> handleExpiredVerificationToken(
            ExpiredVerificationTokenException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VERIFICATION_TOKEN_EXPIRED",
                "The verification link has expired.",
                request
        );
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidVerificationToken(
            InvalidVerificationTokenException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VERIFICATION_TOKEN_INVALID",
                "The verification link is invalid or has already been used.",
                request
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "An account with this email already exists.",
                request
        );
    }

    @ExceptionHandler(VerificationEmailDeliveryException.class)
    public ResponseEntity<ProblemDetail> handleVerificationEmailUnavailable(
            VerificationEmailDeliveryException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "VERIFICATION_EMAIL_UNAVAILABLE",
                "The verification email could not be sent. Please retry later.",
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request data is invalid.",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unhandled request failure types: {}", exceptionTypes(exception));
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "The request could not be completed.",
                request
        );
    }

    private String exceptionTypes(Throwable exception) {
        StringBuilder types = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (!types.isEmpty()) {
                types.append(" -> ");
            }
            types.append(current.getClass().getName());
            current = current.getCause();
        }
        return types.toString();
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", code);
        problem.setProperty(
                "correlationId",
                request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE)
        );

        return ResponseEntity.status(status).body(problem);
    }
}
