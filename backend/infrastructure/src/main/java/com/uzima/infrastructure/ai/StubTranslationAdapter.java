package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.TranslationPort;

/**
 * Adaptateur stub : Traduction no-op (pour dev/test).
 * <p>
 * Remplacer par OpenAITranslationAdapter ou DeepLAdapter en production.
 */
public class StubTranslationAdapter implements TranslationPort {

    @Override
    public String translate(String text, String targetLanguage) {
        return "[Traduction simulée vers " + targetLanguage + "] " + text;
    }
}
