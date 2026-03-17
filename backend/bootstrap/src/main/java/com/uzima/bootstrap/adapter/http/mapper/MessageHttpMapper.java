package com.uzima.bootstrap.adapter.http.mapper;

import com.uzima.application.message.port.in.SendMessageCommand;
import com.uzima.bootstrap.adapter.http.dto.SendMessageRequest;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.List;

/**
 * Mapper HTTP : Conversion entre DTOs HTTP et objets applicatifs/domaine pour la messagerie.
 */
public final class MessageHttpMapper {

    private MessageHttpMapper() {}

    public static SendMessageCommand toSendCommand(
            String conversationId,
            UserId senderId,
            SendMessageRequest request
    ) {
        return SendMessageCommand.text(
                ConversationId.of(conversationId),
                senderId,
                request.content()
        );
    }

    public static MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.id().toString(),
                message.conversationId().toString(),
                message.senderId().toString(),
                message.isDeleted() ? null : message.content().text(),
                message.type().name(),
                message.sentAt(),
                message.isDeleted()
        );
    }

    public static List<MessageResponse> toResponseList(List<Message> messages) {
        return messages.stream().map(MessageHttpMapper::toResponse).toList();
    }

    public record MessageResponse(
            String id,
            String conversationId,
            String senderId,
            String content,
            String type,
            Instant sentAt,
            boolean deleted
    ) {}
}
