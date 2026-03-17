package com.uzima.application.message.port.out;

/** Port OUT : Détection d'intention dans un message texte (NLP). */
public interface IntentDetectionPort {

    /**
     * Détecte l'intention principale du texte.
     *
     * @param text Texte à analyser
     * @return Résultat de détection
     */
    DetectedIntent detect(String text);

    record DetectedIntent(String intent, double confidence) {

        /** Intentions reconnues par le système. */
        public static final String PAYMENT_REQUEST    = "payment_request";
        public static final String MEETING_SCHEDULING = "meeting_scheduling";
        public static final String TASK_ASSIGNMENT    = "task_assignment";
        public static final String EMERGENCY          = "emergency";
        public static final String GREETING           = "greeting";
        public static final String UNKNOWN            = "unknown";

        public static DetectedIntent of(String intent, double confidence) {
            return new DetectedIntent(intent, confidence);
        }

        public static DetectedIntent unknown() {
            return new DetectedIntent(UNKNOWN, 0.0);
        }
    }
}
