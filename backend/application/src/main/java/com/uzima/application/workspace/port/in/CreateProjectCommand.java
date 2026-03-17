package com.uzima.application.workspace.port.in;

import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Création d'un projet. */
public record CreateProjectCommand(String name, UserId requesterId) {

    public CreateProjectCommand {
        Objects.requireNonNull(name,        "Le nom est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
    }
}
