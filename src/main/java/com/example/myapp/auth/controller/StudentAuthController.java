package com.example.myapp.auth.controller;

import com.example.myapp.auth.dto.VerifyCodeRequest;
import com.example.myapp.auth.dto.VerifyCodeResponse;
import com.example.myapp.auth.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생 인증 API.
 *
 * <pre>
 * POST /api/v1/students/verify-code   학생 초대 코드 검증 (교사 연결)
 * </pre>
 *
 * 학생은 비밀번호 없이, 교사가 발급한 임시 초대코드를 입력해 검증한다.
 * (별도의 학생 로그인 엔드포인트는 두지 않음 — 비밀번호 미사용 설계)
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
}
