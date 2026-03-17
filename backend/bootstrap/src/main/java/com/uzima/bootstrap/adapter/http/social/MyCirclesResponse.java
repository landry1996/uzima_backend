package com.uzima.bootstrap.adapter.http.social;

import com.uzima.application.social.GetMyCirclesUseCase.MyCirclesView;

import java.util.List;

/**
 * DTO HTTP sortant : Vue agrégée de tous les cercles d'un utilisateur.
 */
public record MyCirclesResponse(
        List<CircleResponse> owned,
        List<CircleResponse> joined,
        int                  total
) {
    public static MyCirclesResponse from(MyCirclesView view) {
        List<CircleResponse> owned  = view.owned().stream().map(CircleResponse::from).toList();
        List<CircleResponse> joined = view.joined().stream().map(CircleResponse::from).toList();
        return new MyCirclesResponse(owned, joined, owned.size() + joined.size());
    }
}
