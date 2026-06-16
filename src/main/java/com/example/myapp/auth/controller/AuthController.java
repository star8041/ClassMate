package com.example.myapp.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통 인증 API.
 *
 * <pre>
 * POST /api/v1/auth/logout   로그아웃
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * 로그아웃.
     * <p>
     * JWT 는 무상태(stateless)라 서버 세션이 없으므로, 클라이언트가 토큰을 폐기하는 것으로 처리한다.
     * (200 OK 만 반환)
     * TODO: 토큰 블랙리스트/리프레시 토큰 도입 시 서버측 무효화 로직 추가.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout() {
        // no-op (클라이언트 토큰 폐기)
    }
}
