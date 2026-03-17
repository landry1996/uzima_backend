package com.uzima.application.wellbeing.port.in;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.AppType;

import java.util.Objects;

/** Commande : Enregistrer une session d'utilisation d'une application. */
public record TrackAppUsageCommand(UserId userId, String appName, AppType appType) {
    public TrackAppUsageCommand {
        Objects.requireNonNull(userId,   "userId est obligatoire");
        Objects.requireNonNull(appName,  "appName est obligatoire");
        Objects.requireNonNull(appType,  "appType est obligatoire");
    }
}
