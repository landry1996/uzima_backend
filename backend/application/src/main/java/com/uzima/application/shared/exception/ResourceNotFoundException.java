package com.uzima.application.shared.exception;

/**
 * Exception applicative : Ressource non trouvée.
 * Traduite en HTTP 404 par le GlobalExceptionHandler.
 * <p>
 * Utiliser UNIQUEMENT les factory methods pour créer des instances.
 * Le constructeur est privé pour forcer l'utilisation des factories.
 */
public final class ResourceNotFoundException extends ApplicationException {

    private ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    // -------------------------------------------------------------------------
    // Factory Method Générique
    // -------------------------------------------------------------------------

    public static ResourceNotFoundException notFound(String resourceType, Object identifier) {
        String errorCode = resourceType.toUpperCase().replace(" ", "_") + "_NOT_FOUND";
        String message = resourceType + " introuvable : " + identifier;
        return new ResourceNotFoundException(errorCode, message);
    }

    // -------------------------------------------------------------------------
    // Factory Methods Spécifiques (par domaine métier)
    // -------------------------------------------------------------------------

    public static ResourceNotFoundException reminderNotFound(Object reminderId) {
        return notFound("Reminder", reminderId);
    }

    public static ResourceNotFoundException transactionNotFound(Object transactionId) {
        return notFound("Transaction", transactionId);
    }

    public static ResourceNotFoundException messageNotFound(Object messageId) {
        return notFound("Message", messageId);
    }

    public static ResourceNotFoundException conversationNotFound(Object conversationId) {
        return notFound("Conversation", conversationId);
    }

    public static ResourceNotFoundException qrCodeNotFound(Object qrCodeId) {
        return notFound("QR Code", qrCodeId);
    }

    public static ResourceNotFoundException circleNotFound(Object circleId) {
        return notFound("Circle", circleId);
    }

    public static ResourceNotFoundException taskNotFound(Object taskId) {
        return notFound("Task", taskId);
    }

    public static ResourceNotFoundException invoiceNotFound(Object invoiceId) {
        return notFound("Invoice", invoiceId);
    }

    public static ResourceNotFoundException focusSessionNotFound(Object sessionId) {
        return notFound("Focus Session", sessionId);
    }

    public static ResourceNotFoundException projectNotFound(Object projectId) {
        return notFound("Project", projectId);
    }

    public static ResourceNotFoundException timeEntryNotFound(Object timeEntryId) {
        return notFound("Time Entry", timeEntryId);
    }
}
