package com.uzima.bootstrap.adapter.http.exception;

import com.uzima.application.payment.CancelTransactionUseCase;
import com.uzima.application.payment.SendPaymentUseCase;
import com.uzima.application.social.AddMemberToCircleUseCase;
import com.uzima.application.social.RemoveMemberFromCircleUseCase;
import com.uzima.application.message.TranscribeVoiceMessageUseCase;
import com.uzima.application.qrcode.ScanQrCodeUseCase;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.qrcode.model.GeofenceRule;
import com.uzima.domain.qrcode.model.PersonalizationRule;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceItem;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.workspace.model.Project;
import com.uzima.domain.workspace.model.Task;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.application.security.BruteForceProtectionService;
import com.uzima.application.shared.exception.ApplicationException;
import com.uzima.application.shared.exception.ConflictException;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.message.GetConversationUseCase;
import com.uzima.application.message.SendMessageUseCase;
import com.uzima.application.message.StartConversationUseCase;
import com.uzima.application.user.GetUserByIdUseCase;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.application.user.AuthenticateUserUseCase;
import com.uzima.application.user.RegisterUserUseCase;
import com.uzima.application.user.UpdateUserProfileUseCase;
import com.uzima.application.user.port.out.PhoneValidationPort;
import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.exception.BusinessRuleViolationException;
import com.uzima.domain.user.model.CountryCode;
import com.uzima.domain.user.model.FirstName;
import com.uzima.domain.user.model.LastName;
import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.security.token.port.AccessTokenVerifierPort;
import com.uzima.security.token.usecase.RefreshAccessTokenUseCase;
import com.uzima.infrastructure.payment.gateway.DisabledPaymentGatewayAdapter;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import com.uzima.infrastructure.shared.exception.InfrastructureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gestionnaire global des exceptions HTTP.
 * <p>
 * Centralise toute la logique de traduction Exception → HTTP.
 * Les contrôleurs n'ont plus de @ExceptionHandler locaux.
 * <p>
 * Hiérarchie de traduction :
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ DomainException                    → 400 Bad Request / 422          │
 * │   └── BusinessRuleViolationException → 422 Unprocessable Entity     │
 * │   └── InvalidPhoneNumberException   → 400 Bad Request               │
 * │ ApplicationException               → 4xx selon sous-type            │
 * │   └── ResourceNotFoundException    → 404 Not Found                  │
 * │   └── UnauthorizedException        → 403 Forbidden                  │
 * │   └── ConflictException            → 409 Conflict                   │
 * │ AuthenticationFailedException      → 401 Unauthorized               │
 * │ AccountTemporarilyLockedException  → 429 Too Many Requests          │
 * │ InfrastructureException            → 5xx Service Error              │
 * │   └── DatabaseException            → 503 Service Unavailable        │
 * │   └── ExternalServiceException     → 502 Bad Gateway                │
 * │ Validation (@Valid)                → 400 Bad Request                 │
 * │ Exception (catch-all)              → 500 Internal Server Error       │
 * └─────────────────────────────────────────────────────────────────────┘
 * <p>
 * Sécurité : les détails techniques (stack trace, noms internes) ne sont
 * jamais exposés dans la réponse HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());

    // -------------------------------------------------------------------------
    // Exceptions de domaine
    // -------------------------------------------------------------------------

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiError> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warning("Règle métier violée [" + ex.ruleCode() + "] : " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, ex.ruleCode(), ex.getMessage()));
    }

    @ExceptionHandler(PhoneNumber.InvalidPhoneNumberException.class)
    public ResponseEntity<ApiError> handleInvalidPhone(PhoneNumber.InvalidPhoneNumberException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_PHONE_NUMBER", ex.getMessage()));
    }

    @ExceptionHandler(CountryCode.InvalidCountryCodeException.class)
    public ResponseEntity<ApiError> handleInvalidCountry(CountryCode.InvalidCountryCodeException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_COUNTRY_CODE", ex.getMessage()));
    }

    @ExceptionHandler(FirstName.InvalidFirstNameException.class)
    public ResponseEntity<ApiError> handleInvalidFirstName(FirstName.InvalidFirstNameException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_FIRST_NAME", ex.getMessage()));
    }

    @ExceptionHandler(LastName.InvalidLastNameException.class)
    public ResponseEntity<ApiError> handleInvalidLastName(LastName.InvalidLastNameException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_LAST_NAME", ex.getMessage()));
    }

    @ExceptionHandler(PhoneValidationPort.PhoneValidationException.class)
    public ResponseEntity<ApiError> handlePhoneValidation(PhoneValidationPort.PhoneValidationException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "PHONE_COUNTRY_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(DomainException ex) {
        log.warning("Exception de domaine : " + ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "DOMAIN_RULE_VIOLATION", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exceptions d'application
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, ex.errorCode(), ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, ex.errorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, ex.errorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiError> handleApplicationException(ApplicationException ex) {
        log.warning("Exception applicative [" + ex.errorCode() + "] : " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, ex.errorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Authentification & sécurité
    // -------------------------------------------------------------------------

    @ExceptionHandler(AuthenticateUserUseCase.AuthenticationFailedException.class)
    public ResponseEntity<ApiError> handleAuthFailed(AuthenticateUserUseCase.AuthenticationFailedException ex) {
        // Message GÉNÉRIQUE : ne pas révéler si le compte existe ou non
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "AUTHENTICATION_FAILED", "Identifiants invalides"));
    }

    @ExceptionHandler(RegisterUserUseCase.PhoneNumberAlreadyUsedException.class)
    public ResponseEntity<ApiError> handlePhoneAlreadyUsed(RegisterUserUseCase.PhoneNumberAlreadyUsedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "PHONE_ALREADY_USED", ex.getMessage()));
    }

    @ExceptionHandler(RefreshAccessTokenUseCase.TokenRefreshException.class)
    public ResponseEntity<ApiError> handleTokenRefresh(RefreshAccessTokenUseCase.TokenRefreshException ex) {
        // Même message pour token expiré, inconnu ou compromis (pas de fuite d'info)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "TOKEN_REFRESH_FAILED", "Refresh token invalide ou expiré"));
    }

    @ExceptionHandler(AccessTokenVerifierPort.InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(AccessTokenVerifierPort.InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "INVALID_ACCESS_TOKEN", "Access token invalide ou expiré"));
    }

    @ExceptionHandler(SendMessageUseCase.SenderNotInConversationException.class)
    public ResponseEntity<ApiError> handleSenderNotInConversation(SendMessageUseCase.SenderNotInConversationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "SENDER_NOT_IN_CONVERSATION", ex.getMessage()));
    }

    @ExceptionHandler(SendMessageUseCase.VoiceMessageNotAllowedException.class)
    public ResponseEntity<ApiError> handleVoiceNotAllowed(SendMessageUseCase.VoiceMessageNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "VOICE_MESSAGE_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(GetConversationUseCase.UnauthorizedConversationAccessException.class)
    public ResponseEntity<ApiError> handleUnauthorizedConversation(GetConversationUseCase.UnauthorizedConversationAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "CONVERSATION_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(StartConversationUseCase.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundForConversation(StartConversationUseCase.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UpdateUserProfileUseCase.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundForProfile(UpdateUserProfileUseCase.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(GetUserByIdUseCase.UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(GetUserByIdUseCase.UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(SecurityContextHelper.UnauthenticatedException.class)
    public ResponseEntity<ApiError> handleUnauthenticated(SecurityContextHelper.UnauthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "UNAUTHENTICATED", "Authentification requise"));
    }

    @ExceptionHandler(Money.InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(Money.InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "INSUFFICIENT_FUNDS", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exceptions Payment
    // -------------------------------------------------------------------------

    @ExceptionHandler(Transaction.SelfPaymentException.class)
    public ResponseEntity<ApiError> handleSelfPayment(Transaction.SelfPaymentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "SELF_PAYMENT_FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(Transaction.NonPositiveAmountException.class)
    public ResponseEntity<ApiError> handleNonPositiveAmount(Transaction.NonPositiveAmountException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "NON_POSITIVE_AMOUNT", ex.getMessage()));
    }

    @ExceptionHandler(Money.NegativeAmountException.class)
    public ResponseEntity<ApiError> handleNegativeAmount(Money.NegativeAmountException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "NEGATIVE_AMOUNT", ex.getMessage()));
    }

    @ExceptionHandler(Money.CurrencyMismatchException.class)
    public ResponseEntity<ApiError> handleCurrencyMismatch(Money.CurrencyMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "CURRENCY_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(Transaction.IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalTransition(Transaction.IllegalTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "ILLEGAL_TRANSACTION_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(CancelTransactionUseCase.TransactionAccessDeniedException.class)
    public ResponseEntity<ApiError> handleTransactionAccessDenied(
            CancelTransactionUseCase.TransactionAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "TRANSACTION_ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(SendPaymentUseCase.PaymentGatewayException.class)
    public ResponseEntity<ApiError> handlePaymentGateway(SendPaymentUseCase.PaymentGatewayException ex) {
        log.log(Level.SEVERE, "Gateway de paiement indisponible : " + ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(502, "PAYMENT_GATEWAY_ERROR", "Gateway de paiement temporairement indisponible"));
    }

    @ExceptionHandler(DisabledPaymentGatewayAdapter.PaymentGatewayNotConfiguredException.class)
    public ResponseEntity<ApiError> handlePaymentGatewayNotConfigured(
            DisabledPaymentGatewayAdapter.PaymentGatewayNotConfiguredException ex) {
        log.log(Level.WARNING, "Tentative de paiement sans gateway configurée : " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(503, "PAYMENT_NOT_CONFIGURED",
                        "Le service de paiement n'est pas disponible dans cet environnement"));
    }

    // -------------------------------------------------------------------------
    // Exceptions Social (Cercles de Vie)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Circle.InvalidCircleNameException.class)
    public ResponseEntity<ApiError> handleInvalidCircleName(Circle.InvalidCircleNameException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_CIRCLE_NAME", ex.getMessage()));
    }

    @ExceptionHandler(Circle.DuplicateMemberException.class)
    public ResponseEntity<ApiError> handleDuplicateMember(Circle.DuplicateMemberException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "DUPLICATE_CIRCLE_MEMBER", ex.getMessage()));
    }

    @ExceptionHandler(Circle.OwnerCannotLeaveException.class)
    public ResponseEntity<ApiError> handleOwnerCannotLeave(Circle.OwnerCannotLeaveException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "OWNER_CANNOT_LEAVE_CIRCLE", ex.getMessage()));
    }

    @ExceptionHandler(Circle.MemberNotFoundException.class)
    public ResponseEntity<ApiError> handleCircleMemberNotFound(Circle.MemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "CIRCLE_MEMBER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Circle.InsufficientPermissionException.class)
    public ResponseEntity<ApiError> handleInsufficientCirclePermission(Circle.InsufficientPermissionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "INSUFFICIENT_CIRCLE_PERMISSION", ex.getMessage()));
    }

    @ExceptionHandler(AddMemberToCircleUseCase.CircleAccessDeniedException.class)
    public ResponseEntity<ApiError> handleCircleAccessDeniedAdd(AddMemberToCircleUseCase.CircleAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, ex.errorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RemoveMemberFromCircleUseCase.CircleAccessDeniedException.class)
    public ResponseEntity<ApiError> handleCircleAccessDeniedRemove(RemoveMemberFromCircleUseCase.CircleAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, ex.errorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exceptions Workspace
    // -------------------------------------------------------------------------

    @ExceptionHandler(Project.InvalidProjectNameException.class)
    public ResponseEntity<ApiError> handleInvalidProjectName(Project.InvalidProjectNameException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_PROJECT_NAME", ex.getMessage()));
    }

    @ExceptionHandler(Project.InsufficientProjectPermissionException.class)
    public ResponseEntity<ApiError> handleInsufficientProjectPermission(
            Project.InsufficientProjectPermissionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "INSUFFICIENT_PROJECT_PERMISSION", ex.getMessage()));
    }

    @ExceptionHandler(Project.ProjectMembershipRequiredException.class)
    public ResponseEntity<ApiError> handleProjectMembershipRequired(
            Project.ProjectMembershipRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "PROJECT_MEMBERSHIP_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(Project.DuplicateMemberException.class)
    public ResponseEntity<ApiError> handleDuplicateProjectMember(Project.DuplicateMemberException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "DUPLICATE_PROJECT_MEMBER", ex.getMessage()));
    }

    @ExceptionHandler(Project.DuplicateTaskException.class)
    public ResponseEntity<ApiError> handleDuplicateTask(Project.DuplicateTaskException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "DUPLICATE_TASK", ex.getMessage()));
    }

    @ExceptionHandler(Project.ActiveTimeEntryExistsException.class)
    public ResponseEntity<ApiError> handleActiveTimeEntry(Project.ActiveTimeEntryExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "ACTIVE_TIME_ENTRY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(Task.InvalidTaskTitleException.class)
    public ResponseEntity<ApiError> handleInvalidTaskTitle(Task.InvalidTaskTitleException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_TASK_TITLE", ex.getMessage()));
    }

    @ExceptionHandler(Task.IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalTaskTransition(Task.IllegalTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "ILLEGAL_TASK_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(Task.InvalidBlockReasonException.class)
    public ResponseEntity<ApiError> handleInvalidBlockReason(Task.InvalidBlockReasonException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_BLOCK_REASON", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exceptions Invoice
    // -------------------------------------------------------------------------

    @ExceptionHandler(Invoice.SelfInvoicingException.class)
    public ResponseEntity<ApiError> handleSelfInvoicing(Invoice.SelfInvoicingException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "SELF_INVOICING_FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(Invoice.InvoiceAlreadySentException.class)
    public ResponseEntity<ApiError> handleInvoiceAlreadySent(Invoice.InvoiceAlreadySentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "INVOICE_ALREADY_SENT", ex.getMessage()));
    }

    @ExceptionHandler(Invoice.InvoiceCannotBeSentException.class)
    public ResponseEntity<ApiError> handleInvoiceCannotBeSent(Invoice.InvoiceCannotBeSentException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "INVOICE_CANNOT_BE_SENT", ex.getMessage()));
    }

    @ExceptionHandler(Invoice.CurrencyMismatchException.class)
    public ResponseEntity<ApiError> handleInvoiceCurrencyMismatch(Invoice.CurrencyMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVOICE_CURRENCY_MISMATCH", ex.getMessage()));
    }

    @ExceptionHandler(Invoice.IllegalTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalInvoiceTransition(Invoice.IllegalTransitionException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "ILLEGAL_INVOICE_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(InvoiceItem.InvalidItemDescriptionException.class)
    public ResponseEntity<ApiError> handleInvalidItemDescription(InvoiceItem.InvalidItemDescriptionException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_ITEM_DESCRIPTION", ex.getMessage()));
    }

    @ExceptionHandler(InvoiceItem.InvalidQuantityException.class)
    public ResponseEntity<ApiError> handleInvalidQuantity(InvoiceItem.InvalidQuantityException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_ITEM_QUANTITY", ex.getMessage()));
    }

    @ExceptionHandler(com.uzima.domain.invoice.model.TaxRate.InvalidTaxRateException.class)
    public ResponseEntity<ApiError> handleInvalidTaxRate(
            com.uzima.domain.invoice.model.TaxRate.InvalidTaxRateException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_TAX_RATE", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Messagerie IA (Sprint 9-10)
    // -------------------------------------------------------------------------

    @ExceptionHandler(TranscribeVoiceMessageUseCase.NotAVoiceMessageException.class)
    public ResponseEntity<ApiError> handleNotAVoiceMessage(TranscribeVoiceMessageUseCase.NotAVoiceMessageException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "NOT_A_VOICE_MESSAGE", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // QR Code (Sprint 11-12)
    // -------------------------------------------------------------------------

    @ExceptionHandler(QrCode.QrCodeRevokedException.class)
    public ResponseEntity<ApiError> handleQrCodeRevoked(QrCode.QrCodeRevokedException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(422, "QR_CODE_REVOKED", ex.getMessage()));
    }

    @ExceptionHandler(QrCode.QrCodeExpiredException.class)
    public ResponseEntity<ApiError> handleQrCodeExpired(QrCode.QrCodeExpiredException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(422, "QR_CODE_EXPIRED", ex.getMessage()));
    }

    @ExceptionHandler(QrCode.ScanLimitReachedException.class)
    public ResponseEntity<ApiError> handleScanLimitReached(QrCode.ScanLimitReachedException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(422, "QR_CODE_SCAN_LIMIT_REACHED", ex.getMessage()));
    }

    @ExceptionHandler(QrCode.ExpirationRequiredForTypeException.class)
    public ResponseEntity<ApiError> handleExpirationRequired(QrCode.ExpirationRequiredForTypeException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "QR_CODE_EXPIRATION_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(ScanQrCodeUseCase.OutsideGeofenceException.class)
    public ResponseEntity<ApiError> handleOutsideGeofence(ScanQrCodeUseCase.OutsideGeofenceException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "OUTSIDE_GEOFENCE", ex.getMessage()));
    }

    @ExceptionHandler(ScanQrCodeUseCase.GeolocationUnavailableException.class)
    public ResponseEntity<ApiError> handleGeolocationUnavailable(ScanQrCodeUseCase.GeolocationUnavailableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "GEOLOCATION_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(GeofenceRule.InvalidGeofenceException.class)
    public ResponseEntity<ApiError> handleInvalidGeofence(GeofenceRule.InvalidGeofenceException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_GEOFENCE", ex.getMessage()));
    }

    @ExceptionHandler(PersonalizationRule.InvalidPersonalizationRuleException.class)
    public ResponseEntity<ApiError> handleInvalidPersonalization(PersonalizationRule.InvalidPersonalizationRuleException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_PERSONALIZATION_RULE", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Assistant IA — Rappels
    // -------------------------------------------------------------------------

    @ExceptionHandler(Reminder.InvalidReminderContentException.class)
    public ResponseEntity<ApiError> handleInvalidReminderContent(Reminder.InvalidReminderContentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_REMINDER_CONTENT", ex.getMessage()));
    }

    @ExceptionHandler(Reminder.IllegalReminderTransitionException.class)
    public ResponseEntity<ApiError> handleIllegalReminderTransition(Reminder.IllegalReminderTransitionException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(422, "ILLEGAL_REMINDER_TRANSITION", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Wellbeing — Sessions de focus
    // -------------------------------------------------------------------------

    @ExceptionHandler(FocusSession.AlreadyEndedException.class)
    public ResponseEntity<ApiError> handleAlreadyEnded(FocusSession.AlreadyEndedException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(ApiError.of(422, "FOCUS_SESSION_ALREADY_ENDED", ex.getMessage()));
    }

    @ExceptionHandler(UsageSession.InvalidAppNameException.class)
    public ResponseEntity<ApiError> handleInvalidAppName(UsageSession.InvalidAppNameException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "INVALID_APP_NAME", ex.getMessage()));
    }

    @ExceptionHandler(BruteForceProtectionService.AccountTemporarilyLockedException.class)
    public ResponseEntity<ApiError> handleAccountLocked(
            BruteForceProtectionService.AccountTemporarilyLockedException ex
    ) {
        // HTTP 429 Too Many Requests avec durée de verrouillage dans le message
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiError.of(429, "ACCOUNT_TEMPORARILY_LOCKED", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exceptions d'infrastructure (5xx)
    // -------------------------------------------------------------------------

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ApiError> handleDatabase(DatabaseException ex) {
        // Log technique détaillé côté serveur, message générique côté client
        log.log(Level.SEVERE, "Erreur base de données : " + ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(503, "DATABASE_ERROR", "Service temporairement indisponible"));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> handleExternalService(ExternalServiceException ex) {
        log.log(Level.SEVERE, "Service externe [" + ex.serviceName() + "] indisponible : " + ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(502, "EXTERNAL_SERVICE_ERROR", "Service externe temporairement indisponible"));
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiError> handleInfrastructure(InfrastructureException ex) {
        log.log(Level.SEVERE, "Erreur infrastructure : " + ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INFRASTRUCTURE_ERROR", "Erreur interne du serveur"));
    }

    // -------------------------------------------------------------------------
    // Erreurs de validation Bean Validation (@Valid)
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDATION_ERROR", details));
    }

    // -------------------------------------------------------------------------
    // Catch-all (défense en profondeur)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        // Log complet côté serveur
        log.log(Level.SEVERE, "Exception inattendue : " + ex.getMessage(), ex);
        // Message GÉNÉRIQUE côté client (pas de fuite d'information)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "Une erreur interne s'est produite"));
    }
}
