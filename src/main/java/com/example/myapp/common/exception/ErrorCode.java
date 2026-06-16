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
    AI_RESPONSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 생성에 실패했습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "리포트를 찾을 수 없습니다."),
    REPORT_DATA_INSUFFICIENT(HttpStatus.BAD_REQUEST, "리포트 생성에 필요한 데이터가 없습니다. 해당 기간의 퀴즈 응시 또는 상담 기록을 확인해 주세요."),

    // 인증 / 사용자 / 관리자
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    SIGNUP_INVALID(HttpStatus.BAD_REQUEST, "이름/아이디/비밀번호는 필수입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 초대 코드입니다."),
    INVITE_CODE_TEACHER_NOT_FOUND(HttpStatus.BAD_REQUEST, "초대 코드에 연결된 교사를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
