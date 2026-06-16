package com.example.myapp.auth.controller;

import com.example.myapp.auth.dto.InviteCodeResponse;
import com.example.myapp.auth.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교사 초대코드 API.
 *
 * <pre>
 * POST /api/v1/teachers/me/invite-code   현재 교사의 임시 초대코드 생성/재발급
 * </pre>
 *
 * 현재 교사(me)는 Authorization 헤더의 JWT 로 식별한다.
 * 코드는 서버 메모리에 TTL(기본 10분) 동안만 보관된다 (DB 저장 없음).
 */
@RestController
@RequestMapping("/api/v1/teachers/me/invite-code")
public class TeacherInviteCodeController {

    private final AuthService authService;

    public TeacherInviteCodeController(AuthService authService) {
        this.authService = authService;
    }

    /** 초대코드 생성/재발급 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InviteCodeResponse generate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return authService.generateInviteCode(authHeader);
    }
}
