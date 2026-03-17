package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.IntentDetectionPort;

/**
 * Adaptateur stub : Détection d'intention no-op (pour dev/test).
 * <p>
 * Remplacer par OpenAIIntentDetectionAdapter en production.
 * Utilise des heuristiques simples basées sur des mots-clés.
 */
public class StubIntentDetectionAdapter implements IntentDetectionPort {

    @Override
    public DetectedIntent detect(String text) {
        if (text == null || text.isBlank()) {
            return DetectedIntent.unknown();
        }

        String lower = text.toLowerCase();

        return switch (findMatchingIntent(lower)) {
            case PAYMENT_REQUEST -> DetectedIntent.of(DetectedIntent.PAYMENT_REQUEST, 0.85);
            case MEETING_SCHEDULING -> DetectedIntent.of(DetectedIntent.MEETING_SCHEDULING, 0.80);
            case EMERGENCY -> DetectedIntent.of(DetectedIntent.EMERGENCY, 0.90);
            case GREETING -> DetectedIntent.of(DetectedIntent.GREETING, 0.75);
            case TASK_ASSIGNMENT -> DetectedIntent.of(DetectedIntent.TASK_ASSIGNMENT, 0.70);
            case UNKNOWN -> DetectedIntent.unknown();
        };
    }

    private IntentType findMatchingIntent(String lowerText) {
        return switch (lowerText) {
            case String s when containsAny(s, "payer", "envoyer", "virement", "pay", "send money")
                    -> IntentType.PAYMENT_REQUEST;
            case String s when containsAny(s, "réunion", "rendez-vous", "agenda", "meeting", "schedule")
                    -> IntentType.MEETING_SCHEDULING;
            case String s when containsAny(s, "urgent", "urgence", "emergency")
                    -> IntentType.EMERGENCY;
            case String s when containsAny(s, "bonjour", "salut", "hello", "hi ")
                    -> IntentType.GREETING;
            case String s when containsAny(s, "faire", "tâche", "task", "assign")
                    -> IntentType.TASK_ASSIGNMENT;
            default -> IntentType.UNKNOWN;
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private enum IntentType {
        PAYMENT_REQUEST, MEETING_SCHEDULING, EMERGENCY, GREETING, TASK_ASSIGNMENT, UNKNOWN
    }
}
