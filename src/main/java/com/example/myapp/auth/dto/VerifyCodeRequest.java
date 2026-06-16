package com.example.myapp.auth.dto;

/**
 * 학생 초대코드 검증 요청 DTO.
 */
public record VerifyCodeRequest(
        String code
) {
}
