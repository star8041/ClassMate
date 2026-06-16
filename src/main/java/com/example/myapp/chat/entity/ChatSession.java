package com.example.myapp.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * chat_session 테이블과 1:1 매핑.
 * user_id + role 조합으로 교사/학생을 구분한다 (FK 없음).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    private Long chatSessionId;
    private Long userId;
    private ChatRole role;
    private Long materialId;
    private String title;
    private LocalDateTime createdAt;
}
