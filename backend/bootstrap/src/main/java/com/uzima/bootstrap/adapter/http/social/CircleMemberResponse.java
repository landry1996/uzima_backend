package com.uzima.bootstrap.adapter.http.social;

import com.uzima.domain.social.model.CircleMembership;

import java.time.Instant;

/**
 * DTO HTTP sortant : Membre d'un cercle.
 */
public record CircleMemberResponse(
        String  memberId,
        String  role,
        Instant joinedAt
) {
    public static CircleMemberResponse from(CircleMembership m) {
        return new CircleMemberResponse(
            m.memberId().toString(),
            m.role().name(),
            m.joinedAt()
        );
    }
}
