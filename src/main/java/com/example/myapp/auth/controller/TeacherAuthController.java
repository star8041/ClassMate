package com.example.myapp.auth.controller;

import com.example.myapp.auth.dto.LoginRequest;
import com.example.myapp.auth.dto.TokenResponse;
import com.example.myapp.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교사 인증 API.
 *
 * <pre>
 * POST /api/v1/teacher-auth/login   선생님 로그인
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/teacher-auth")
public class TeacherAuthController {

    private final AuthService authService;

    public TeacherAuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 선생님 로그인 → JWT 발급 */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.loginTeacher(request);
    }
}
