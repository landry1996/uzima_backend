package com.uzima.application.message.port.out;

/** Port OUT : Traduction de texte (ex. OpenAI GPT / DeepL). */
public interface TranslationPort {

    /**
     * Traduit le texte source vers la langue cible.
     *
     * @param text           Texte à traduire
     * @param targetLanguage Code langue ISO 639-1 (ex. "fr", "en", "ar")
     * @return Texte traduit
     */
    String translate(String text, String targetLanguage);
}
