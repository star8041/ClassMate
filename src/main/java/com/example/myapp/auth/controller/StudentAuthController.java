package com.example.myapp.auth.controller;

import com.example.myapp.auth.dto.StudentLoginRequest;
import com.example.myapp.auth.dto.TokenResponse;
import com.example.myapp.auth.dto.VerifyCodeRequest;
import com.example.myapp.auth.dto.VerifyCodeResponse;
import com.example.myapp.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생 인증 API. (PDF 명세서 기준)
 *
 * <pre>
 * POST /api/v1/students/verify-code   학생 초대 코드 검증 (교사 연결)
 * POST /api/v1/students/login         학생 로그인
 * </pre>
 *
 * 학생은 비밀번호 없이 교사가 발급한 임시 초대코드를 사용한다.
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentAuthController {

    private final AuthService authService;

    public StudentAuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 학생 초대 코드 검증 → 유효하면 연결될 교사 정보 반환 */
    @PostMapping("/verify-code")
    public VerifyCodeResponse verifyCode(@RequestBody VerifyCodeRequest request) {
        return authService.verifyInviteCode(request.code());
    }

    /**
     * 학생 로그인.
     * <p>
     * 이름 + 학번 + 담임선생님이 발급한 초대코드를 검증하여 JWT를 발급한다.
     * 초대코드는 발급 교사와 student.teacher_id 가 일치해야 하며,
     * 만료된 코드는 거부된다.
     */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody StudentLoginRequest request) {
        return authService.loginStudent(request);
    }
}
