package com.example.myapp.auth.dto;

import java.time.Instant;

/**
 * 교사 초대코드 생성 응답 DTO.
 */
public record InviteCodeResponse(
        String code,
        Instant expiresAt
) {
}
