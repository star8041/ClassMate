package com.example.myapp.chat.dto;

import com.example.myapp.chat.entity.ChatSession;

import java.time.LocalDateTime;

public record ChatSessionResponse(
        Long chatSessionId,
        Long materialId,
        String title,
        LocalDateTime createdAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getChatSessionId(),
                session.getMaterialId(),
                session.getTitle(),
                session.getCreatedAt()
        );
    }
}
