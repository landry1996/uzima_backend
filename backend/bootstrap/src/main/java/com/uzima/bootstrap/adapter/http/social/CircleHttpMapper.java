package com.uzima.bootstrap.adapter.http.social;

import com.uzima.application.social.port.in.AddMemberCommand;
import com.uzima.application.social.port.in.CreateCircleCommand;
import com.uzima.application.social.port.in.RemoveMemberCommand;
import com.uzima.application.social.port.in.RenameCircleCommand;
import com.uzima.application.social.port.in.UpdateCircleRulesCommand;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.CircleRule;
import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.social.model.MemberRole;
import com.uzima.domain.social.model.NotificationPolicy;
import com.uzima.domain.social.model.VisibilityLevel;
import com.uzima.domain.user.model.UserId;

/**
 * Mapper HTTP ↔ Application : Cercles de Vie.
 *
 * Convertit les DTOs HTTP en commandes application.
 * La conversion des enums (CircleType, MemberRole, etc.) lève IllegalArgumentException
 * → traduite en 400 Bad Request par le GlobalExceptionHandler.
 */
public final class CircleHttpMapper {

    private CircleHttpMapper() {}

    public static CreateCircleCommand toCreateCommand(CreateCircleRequest request, UserId requesterId) {
        return new CreateCircleCommand(
            requesterId,
            request.name(),
            CircleType.valueOf(request.type().toUpperCase())
        );
    }

    public static AddMemberCommand toAddMemberCommand(
            AddMemberRequest request, CircleId circleId, UserId requesterId
    ) {
        return new AddMemberCommand(
            circleId,
            requesterId,
            UserId.of(request.newMemberId()),
            MemberRole.valueOf(request.role().toUpperCase())
        );
    }

    public static RemoveMemberCommand toRemoveMemberCommand(
            CircleId circleId, UserId requesterId, UserId targetMemberId
    ) {
        return new RemoveMemberCommand(circleId, requesterId, targetMemberId);
    }

    public static UpdateCircleRulesCommand toUpdateRulesCommand(
            UpdateCircleRulesRequest request, CircleId circleId, UserId requesterId
    ) {
        CircleRule newRules = new CircleRule(
            NotificationPolicy.valueOf(request.notificationPolicy().toUpperCase()),
            VisibilityLevel.valueOf(request.visibility().toUpperCase()),
            request.allowsVoiceMessages(),
            request.allowsPayments()
        );
        return new UpdateCircleRulesCommand(circleId, requesterId, newRules);
    }

    public static RenameCircleCommand toRenameCommand(
            RenameCircleRequest request, CircleId circleId, UserId requesterId
    ) {
        return new RenameCircleCommand(circleId, requesterId, request.newName());
    }
}
