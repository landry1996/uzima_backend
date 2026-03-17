package com.uzima.bootstrap.adapter.http.social;

import com.uzima.application.social.AddMemberToCircleUseCase;
import com.uzima.application.social.CreateCircleUseCase;
import com.uzima.application.social.GetMyCirclesUseCase;
import com.uzima.application.social.RemoveMemberFromCircleUseCase;
import com.uzima.application.social.RenameCircleUseCase;
import com.uzima.application.social.SuggestCircleForContactUseCase;
import com.uzima.application.social.UpdateCircleRulesUseCase;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.user.model.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Adaptateur HTTP entrant : Cercles de Vie (Social Domain).
 * <p>
 * Responsabilités strictes :
 * - Lier les requêtes HTTP aux commandes applicatives
 * - Déléguer aux Use Cases via CircleHttpMapper
 * - Retourner les réponses HTTP avec les bons codes statut
 * <p>
 * Pas de logique métier ici. Pas de @ExceptionHandler (centralisé dans GlobalExceptionHandler).
 * <p>
 * L'identité de l'utilisateur est extraite du JWT via {@link SecurityContextHelper}.
 */
@RestController
@RequestMapping("/api/circles")
public class CircleController {

    private final CreateCircleUseCase          createCircleUseCase;
    private final AddMemberToCircleUseCase     addMemberUseCase;
    private final RemoveMemberFromCircleUseCase removeMemberUseCase;
    private final UpdateCircleRulesUseCase     updateRulesUseCase;
    private final RenameCircleUseCase          renameCircleUseCase;
    private final GetMyCirclesUseCase          getMyCirclesUseCase;
    private final SuggestCircleForContactUseCase suggestUseCase;

    public CircleController(
            CreateCircleUseCase          createCircleUseCase,
            AddMemberToCircleUseCase     addMemberUseCase,
            RemoveMemberFromCircleUseCase removeMemberUseCase,
            UpdateCircleRulesUseCase     updateRulesUseCase,
            RenameCircleUseCase          renameCircleUseCase,
            GetMyCirclesUseCase          getMyCirclesUseCase,
            SuggestCircleForContactUseCase suggestUseCase
    ) {
        this.createCircleUseCase  = Objects.requireNonNull(createCircleUseCase);
        this.addMemberUseCase     = Objects.requireNonNull(addMemberUseCase);
        this.removeMemberUseCase  = Objects.requireNonNull(removeMemberUseCase);
        this.updateRulesUseCase   = Objects.requireNonNull(updateRulesUseCase);
        this.renameCircleUseCase  = Objects.requireNonNull(renameCircleUseCase);
        this.getMyCirclesUseCase  = Objects.requireNonNull(getMyCirclesUseCase);
        this.suggestUseCase       = Objects.requireNonNull(suggestUseCase);
    }

    /**
     * POST /api/circles
     * Crée un nouveau Cercle de Vie.
     * 201 Created + circleId
     */
    @PostMapping
    public ResponseEntity<CircleIdResponse> createCircle(
            @Valid @RequestBody CreateCircleRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command  = CircleHttpMapper.toCreateCommand(request, requesterId);
        var circleId = createCircleUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new CircleIdResponse(circleId.toString()));
    }

    /**
     * GET /api/circles
     * Retourne tous les cercles de l'utilisateur courant (owned + joined).
     * 200 OK + MyCirclesResponse
     */
    @GetMapping
    public ResponseEntity<MyCirclesResponse> getMyCircles() {
        UserId userId = SecurityContextHelper.currentUserId();
        var view = getMyCirclesUseCase.execute(userId);
        return ResponseEntity.ok(MyCirclesResponse.from(view));
    }

    /**
     * POST /api/circles/{id}/members
     * Ajoute un membre dans le cercle (ADMIN+ requis).
     * 204 No Content
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable String id,
            @Valid @RequestBody AddMemberRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = CircleHttpMapper.toAddMemberCommand(
            request, CircleId.of(id), requesterId
        );
        addMemberUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/circles/{id}/members/{userId}
     * Retire un membre du cercle.
     * Auto-retrait libre ; retrait par autrui = ADMIN+ requis.
     * 204 No Content
     */
    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String id,
            @PathVariable String memberId
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = CircleHttpMapper.toRemoveMemberCommand(
            CircleId.of(id), requesterId, UserId.of(memberId)
        );
        removeMemberUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/circles/{id}/rules
     * Met à jour les règles du cercle (ADMIN+ requis).
     * 204 No Content
     */
    @PutMapping("/{id}/rules")
    public ResponseEntity<Void> updateRules(
            @PathVariable String id,
            @Valid @RequestBody UpdateCircleRulesRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = CircleHttpMapper.toUpdateRulesCommand(
            request, CircleId.of(id), requesterId
        );
        updateRulesUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/circles/{id}/name
     * Renomme le cercle (ADMIN+ requis).
     * 204 No Content
     */
    @PatchMapping("/{id}/name")
    public ResponseEntity<Void> rename(
            @PathVariable String id,
            @Valid @RequestBody RenameCircleRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = CircleHttpMapper.toRenameCommand(
            request, CircleId.of(id), requesterId
        );
        renameCircleUseCase.execute(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/circles/suggest?contactId={contactId}
     * Suggère le type de cercle le plus adapté pour un contact.
     * 200 OK + CircleSuggestionResponse
     */
    @GetMapping("/suggest")
    public ResponseEntity<CircleSuggestionResponse> suggestCircle(
            @RequestParam String contactId
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var suggestion = suggestUseCase.execute(requesterId, UserId.of(contactId));
        return ResponseEntity.ok(CircleSuggestionResponse.from(suggestion));
    }

    // -------------------------------------------------------------------------
    // DTO de réponse minimal
    // -------------------------------------------------------------------------

    /** Identifiant du cercle créé. */
    public record CircleIdResponse(String circleId) {}
}
