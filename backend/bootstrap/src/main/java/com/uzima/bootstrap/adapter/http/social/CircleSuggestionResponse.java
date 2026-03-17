package com.uzima.bootstrap.adapter.http.social;

import com.uzima.application.social.SuggestCircleForContactUseCase.CircleSuggestion;

/**
 * DTO HTTP sortant : Suggestion de type de cercle pour un contact.
 */
public record CircleSuggestionResponse(
        String suggestedType,
        String reason
) {
    public static CircleSuggestionResponse from(CircleSuggestion suggestion) {
        return new CircleSuggestionResponse(
            suggestion.suggestedType().name(),
            suggestion.reason()
        );
    }
}
