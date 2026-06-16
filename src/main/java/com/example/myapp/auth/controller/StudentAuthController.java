package com.example.myapp.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 학생 인증 API.
 *
 * <pre>
 * POST /api/v1/students/verify-code   학생 초대 코드 검증
 * POST /api/v1/students/login         학생 로그인
 * </pre>
 *
 * <b>현재 미구현(501).</b>
 * 학생 인증은 다음 스키마가 필요하지만 현재 DDL 에 없다:
 * <ul>
 *     <li>teacher 의 초대 코드(invite_code) 컬럼 — 코드 검증/학생-교사 연결용</li>
 *     <li>student 의 로그인 자격(login_id/password 등) 컬럼 — 학생 로그인용</li>
 * </ul>
 * TODO: 위 스키마(마이그레이션) 확정 후 verify-code / login 구현.
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentAuthController {

    /** 학생 초대 코드 검증 (미구현) */
    @PostMapping("/verify-code")
    public void verifyCode() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "학생 초대 코드 검증은 초대 코드 스키마 확정 후 구현 예정입니다.");
    }

    /** 학생 로그인 (미구현) */
    @PostMapping("/login")
    public void login() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "학생 로그인은 학생 자격(credential) 스키마 확정 후 구현 예정입니다.");
    }
}
