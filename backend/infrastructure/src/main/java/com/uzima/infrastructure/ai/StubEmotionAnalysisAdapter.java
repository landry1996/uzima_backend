package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.EmotionAnalysisPort;

/**
 * Adaptateur stub : Analyse émotionnelle no-op (pour dev/test).
 * <p>
 * Retourne toujours NEUTRAL — remplacer par un vrai modèle audio en production.
 */
public class StubEmotionAnalysisAdapter implements EmotionAnalysisPort {

    @Override
    public EmotionResult analyze(String audioUrl) {
        return EmotionResult.neutral();
    }
}
