package com.uzima.application.wellbeing;

import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Récupérer l'historique des sessions de focus d'un utilisateur selon leur statut.
 * Typiquement utilisé pour afficher l'historique COMPLETED ou INTERRUPTED dans le dashboard bien-être.
 */
public class GetFocusSessionHistoryUseCase {

    private final FocusSessionRepositoryPort focusRepository;

    public GetFocusSessionHistoryUseCase(FocusSessionRepositoryPort focusRepository) {
        this.focusRepository = Objects.requireNonNull(focusRepository, "focusRepository est obligatoire");
    }

    public List<FocusSession> execute(UserId userId, FocusSessionStatus status) {
        Objects.requireNonNull(userId,  "userId est obligatoire");
        Objects.requireNonNull(status,  "status est obligatoire");
        return focusRepository.findByUserIdAndStatus(userId, status);
    }
}
