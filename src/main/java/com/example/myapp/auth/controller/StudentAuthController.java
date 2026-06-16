package com.example.myapp.auth.controller;

import com.example.myapp.auth.dto.VerifyCodeRequest;
import com.example.myapp.auth.dto.VerifyCodeResponse;
import com.example.myapp.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
     * 학생 비밀번호가 없는 설계라 로그인 방식(코드 기반/세션 발급 등)이 아직 확정되지 않았다.
     * 엔드포인트는 PDF 명세에 맞춰 유지하되, 동작은 설계 확정 후 구현한다.
     * TODO: 학생 로그인 방식 확정 후 구현 (예: 초대코드 → 학생 식별 → 토큰 발급)
     */
    @PostMapping("/login")
    public void login() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "학생 로그인 방식 확정 후 구현 예정입니다. (학생 비밀번호 미사용 설계)");
    }
}
