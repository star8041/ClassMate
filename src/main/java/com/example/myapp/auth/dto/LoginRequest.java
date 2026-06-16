package com.example.myapp.auth.dto;

/**
 * 로그인 요청 DTO.
 */
public record LoginRequest(
        String loginId,
        String password
) {
}
