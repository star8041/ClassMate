package com.example.myapp.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * chat_message 테이블과 1:1 매핑.
 * role = TEACHER | STUDENT | AI
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private Long chatMessageId;
    private Long chatSessionId;
    private ChatRole role;
    private String messageText;
    private LocalDateTime createdAt;
}
