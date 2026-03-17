package com.uzima.bootstrap.adapter.http.dto;

import com.uzima.domain.user.model.User;

/**
 * DTO HTTP sortant : Représentation publique d'un utilisateur.
 */
public record UserResponse(
        String id,
        String phoneNumber,
        String countryCode,
        String firstName,
        String lastName,
        String avatarUrl,
        String presenceStatus,
        boolean premium
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().toString(),
                user.phoneNumber().value(),
                user.country().value(),
                user.firstName().value(),
                user.lastName().value(),
                user.avatarUrl().orElse(null),
                user.presenceStatus().name(),
                user.isPremium()
        );
    }
}
