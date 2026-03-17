package com.uzima.application.shared.exception;

/**
 * Exception applicative : Accès non autorisé.
 * Traduite en HTTP 403 Forbidden par le GlobalExceptionHandler.
 * <p>
 * Différence avec AuthenticationFailedException :
 * - AuthenticationFailedException (401) → identifiants invalides (qui êtes-vous ?)
 * - UnauthorizedException (403) → identifié mais pas autorisé (que pouvez-vous faire ?)
 * <p>
 * Utiliser UNIQUEMENT les factory methods pour créer des instances.
 */
public class UnauthorizedException extends ApplicationException {

    protected UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }

    // -------------------------------------------------------------------------
    // Factory Method Générique
    // -------------------------------------------------------------------------

    public static UnauthorizedException accessDenied(String resourceType, Object resourceId) {
        return new UnauthorizedException(
                "ACCESS_DENIED",
                "Accès refusé à la ressource " + resourceType + " : " + resourceId
        );
    }

    // -------------------------------------------------------------------------
    // Factory Methods Spécifiques (cas métier)
    // -------------------------------------------------------------------------

    public static UnauthorizedException notConversationParticipant(Object conversationId) {
        return new UnauthorizedException(
                "NOT_CONVERSATION_PARTICIPANT",
                "Vous n'êtes pas participant de la conversation " + conversationId
        );
    }

    public static UnauthorizedException cannotRevokeOthersQrCode(Object qrCodeId) {
        return new UnauthorizedException(
                "CANNOT_REVOKE_OTHERS_QRCODE",
                "Vous ne pouvez pas révoquer le QR Code " + qrCodeId + " d'un autre utilisateur"
        );
    }

    public static UnauthorizedException notReminderOwner(Object reminderId) {
        return new UnauthorizedException(
                "NOT_REMINDER_OWNER",
                "Vous n'êtes pas propriétaire du rappel " + reminderId
        );
    }

    public static UnauthorizedException notInvoiceIssuer(Object invoiceId) {
        return new UnauthorizedException(
                "NOT_INVOICE_ISSUER",
                "Seul l'émetteur peut effectuer cette opération sur la facture " + invoiceId
        );
    }

    public static UnauthorizedException cannotEditOthersQrCode(Object qrCodeId) {
        return new UnauthorizedException(
                "CANNOT_EDIT_OTHERS_QRCODE",
                "Vous ne pouvez pas modifier le QR Code " + qrCodeId + " d'un autre utilisateur"
        );
    }

    public static UnauthorizedException notFocusSessionOwner(Object sessionId) {
        return new UnauthorizedException(
                "NOT_FOCUS_SESSION_OWNER",
                "Vous n'êtes pas propriétaire de la session de focus " + sessionId
        );
    }

    public static UnauthorizedException cannotStopOthersTimeEntry(Object timeEntryId) {
        return new UnauthorizedException(
                "CANNOT_STOP_OTHERS_TIME_ENTRY",
                "Vous ne pouvez arrêter que vos propres entrées de temps : " + timeEntryId
        );
    }
}
