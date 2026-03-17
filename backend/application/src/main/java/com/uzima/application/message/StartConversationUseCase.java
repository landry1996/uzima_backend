package com.uzima.application.message;

import com.uzima.application.message.port.in.StartConversationCommand;
import com.uzima.application.message.port.out.ConversationRepositoryPort;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Use Case : Démarrer ou retrouver une conversation directe.
 * <p>
 * Règle métier centrale (enforced via findDirectConversation) :
 * "Il ne peut exister qu'une seule conversation directe entre deux utilisateurs."
 * → findDirectConversation détecte le doublon avant la création.
 * <p>
 * Flux :
 * 1. Vérifier que les deux participants existent
 * 2. Chercher une conversation directe existante (évite le doublon)
 * 3. Si elle n'existe pas, la créer via Conversation.createDirect()
 * 4. Persister et retourner
 * <p>
 * Pas de Spring. Pas de framework.
 */
public final class StartConversationUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final UserRepositoryPort userRepository;
    private final TimeProvider clock;

    public StartConversationUseCase(
            ConversationRepositoryPort conversationRepository,
            UserRepositoryPort userRepository,
            TimeProvider clock
    ) {
        this.conversationRepository = Objects.requireNonNull(conversationRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * @param command Contient les UserId des deux participants
     * @return La conversation (nouvelle ou existante)
     */
    public Conversation execute(StartConversationCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        UserId initiatorId = command.initiatorId();
        UserId targetId = command.targetId();

        // 1. Vérifier l'existence des deux utilisateurs
        if (userRepository.findById(initiatorId).isEmpty()) {
            throw new UserNotFoundException("Initiateur introuvable : " + initiatorId);
        }
        if (userRepository.findById(targetId).isEmpty()) {
            throw new UserNotFoundException("Destinataire introuvable : " + targetId);
        }

        // 2. Chercher une conversation directe existante
        //    → findDirectConversation justifie son existence ici
        return conversationRepository.findDirectConversation(initiatorId, targetId)
                .orElseGet(() -> {
                    // 3. Créer via factory method du domaine (invariants garantis)
                    Conversation conversation = Conversation.createDirect(initiatorId, targetId, clock);
                    // 4. Persister
                    conversationRepository.save(conversation);
                    return conversation;
                });
    }

    public static final class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
}
