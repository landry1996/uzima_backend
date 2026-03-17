package com.uzima.application.message.port.out;

/** Port OUT : Analyse émotionnelle d'un contenu audio. */
public interface EmotionAnalysisPort {

    /**
     * Analyse l'émotion dans un message vocal.
     *
     * @param audioUrl URL de l'audio à analyser
     * @return Résultat de l'analyse émotionnelle
     */
    EmotionResult analyze(String audioUrl);

    record EmotionResult(String primaryEmotion, double confidence) {

        public static final String STRESS  = "stress";
        public static final String SADNESS = "sadness";
        public static final String ANGER   = "anger";
        public static final String NEUTRAL = "neutral";
        public static final String FEAR    = "fear";

        public static EmotionResult of(String emotion, double confidence) {
            return new EmotionResult(emotion, confidence);
        }

        public static EmotionResult neutral() {
            return new EmotionResult(NEUTRAL, 1.0);
        }
    }
}
