package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.EmotionAnalysisPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptateur production : Analyse émotionnelle.
 * <p>
 * L'analyse émotionnelle sur audio nécessite un modèle spécialisé (Hume AI, SpeechBrain, etc.)
 * qui n'est pas disponible via l'API OpenAI standard.
 * <p>
 * Cette implémentation retourne {@link EmotionResult#neutral()} jusqu'à ce qu'un
 * fournisseur d'analyse émotionnelle soit configuré.
 * <p>
 * Pour activer :
 * <ol>
 *   <li>Intégrer Hume AI (https://hume.ai) ou un modèle audio équivalent</li>
 *   <li>Configurer {@code uzima.ai.emotion.api-key}</li>
 *   <li>Remplacer cette implémentation par {@code HumeEmotionAnalysisAdapter}</li>
 * </ol>
 */
public class NeutralEmotionAnalysisAdapter implements EmotionAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(NeutralEmotionAnalysisAdapter.class);

    @Override
    public EmotionResult analyze(String audioUrl) {
        log.debug("[EMOTION] Analyse émotionnelle non disponible — retour NEUTRAL pour {}", audioUrl);
        return EmotionResult.neutral();
    }
}
