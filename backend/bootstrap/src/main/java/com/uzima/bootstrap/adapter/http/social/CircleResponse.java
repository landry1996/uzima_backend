package com.uzima.bootstrap.adapter.http.social;

import com.uzima.domain.social.model.Circle;

import java.time.Instant;
import java.util.List;

/**
 * DTO HTTP sortant : Détail d'un Cercle de Vie (incluant les membres).
 */
public record CircleResponse(
        String                    id,
        String                    name,
        String                    type,
        String                    ownerId,
        int                       memberCount,
        CircleRulesResponse       rules,
        List<CircleMemberResponse> members,
        Instant                   createdAt
) {
    public static CircleResponse from(Circle circle) {
        return new CircleResponse(
            circle.id().toString(),
            circle.name(),
            circle.type().name(),
            circle.ownerId().toString(),
            circle.memberCount(),
            CircleRulesResponse.from(circle),
            circle.memberships().stream().map(CircleMemberResponse::from).toList(),
            circle.createdAt()
        );
    }

    /** Sous-DTO : règles du cercle. */
    public record CircleRulesResponse(
            String  notificationPolicy,
            String  visibility,
            boolean allowsVoiceMessages,
            boolean allowsPayments
    ) {
        public static CircleRulesResponse from(Circle circle) {
            var rules = circle.rules();
            return new CircleRulesResponse(
                rules.notificationPolicy().name(),
                rules.visibility().name(),
                rules.allowsVoiceMessages(),
                rules.allowsPayments()
            );
        }
    }
}
