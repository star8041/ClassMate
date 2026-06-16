package com.example.myapp.auth.dto;

/**
 * 로그인 성공 시 발급되는 토큰 응답 DTO.
 */
public record TokenResponse(
        String accessToken,
        String tokenType
) {
    public static TokenResponse bearer(String accessToken) {
        return new TokenResponse(accessToken, "Bearer");
    }
}
