package com.example.myapp.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "자료를 찾을 수 없습니다."),
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."),
    CHAT_SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅 세션에 대한 접근 권한이 없습니다."),
    AI_INTENT_CLASSIFICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "의도 분류에 실패했습니다."),
    AI_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
