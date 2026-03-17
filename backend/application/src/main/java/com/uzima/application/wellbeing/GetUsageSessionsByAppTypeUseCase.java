package com.uzima.application.wellbeing;

import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.AppType;
import com.uzima.domain.wellbeing.model.UsageSession;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Récupérer les sessions d'utilisation d'un utilisateur filtrées par type d'application.
 * Typiquement utilisé pour afficher le temps passé par catégorie (SOCIAL, PRODUCTIVITY, etc.).
 */
public class GetUsageSessionsByAppTypeUseCase {

    private final UsageSessionRepositoryPort usageRepository;

    public GetUsageSessionsByAppTypeUseCase(UsageSessionRepositoryPort usageRepository) {
        this.usageRepository = Objects.requireNonNull(usageRepository, "usageRepository est obligatoire");
    }

    public List<UsageSession> execute(UserId userId, AppType appType) {
        Objects.requireNonNull(userId,  "userId est obligatoire");
        Objects.requireNonNull(appType, "appType est obligatoire");
        return usageRepository.findByUserIdAndAppType(userId, appType);
    }
}
