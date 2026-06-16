package com.example.myapp.chat.dto;

import com.example.myapp.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long chatMessageId,
        String role,
        String messageText,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(
                m.getChatMessageId(),
                m.getRole().name(),
                m.getMessageText(),
                m.getCreatedAt()
        );
    }
}
