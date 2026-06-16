package com.example.myapp.auth.dto;

/**
 * 학생 초대코드 검증 응답 DTO.
 * 코드가 유효하면 연결될 교사 정보를 돌려준다.
 */
public record VerifyCodeResponse(
        boolean valid,
        Long teacherId,
        String teacherName
) {
}
