package com.uzima.application.message.port.out;

import java.util.List;

/** Port OUT : Résumé intelligent d'une conversation (ex. GPT-4). */
public interface ConversationSummaryPort {

    /**
     * Génère un résumé des messages fournis.
     *
     * @param messages Liste de textes formatés ("Sender: contenu")
     * @param language Code langue du résumé souhaité (ex. "fr")
     * @return Résumé en langage naturel
     */
    String summarize(List<String> messages, String language);
}
