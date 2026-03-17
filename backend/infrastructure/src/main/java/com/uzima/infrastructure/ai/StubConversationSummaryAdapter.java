package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.ConversationSummaryPort;

import java.util.List;

/**
 * Adaptateur stub : Résumé de conversation no-op (pour dev/test).
 * <p>
 * Remplacer par OpenAIConversationSummaryAdapter (GPT-4) en production.
 */
public class StubConversationSummaryAdapter implements ConversationSummaryPort {

    @Override
    public String summarize(List<String> messages, String language) {
        return "[Résumé simulé — " + messages.size() + " messages en " + language + "]";
    }
}
