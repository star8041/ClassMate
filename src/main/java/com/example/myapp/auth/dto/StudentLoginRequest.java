package com.example.myapp.auth.dto;

/**
 * 학생 로그인 요청 DTO.
 * 학생은 비밀번호 대신 담임선생님이 발급한 초대코드로 본인을 인증한다.
 */
public record StudentLoginRequest(
        String studentName,
        String studentNumber,
        String inviteCode
) {
}
