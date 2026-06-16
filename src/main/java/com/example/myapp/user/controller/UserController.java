package com.example.myapp.user.controller;

import com.example.myapp.auth.service.AuthService;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.user.dto.MeResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자(내 정보) API.
 *
 * <pre>
 * GET /api/v1/users/me   내 정보 조회 (Authorization: Bearer 토큰 필요)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /** 내 정보 조회: Bearer 토큰으로 현재 교사를 식별해 반환한다. */
    @GetMapping("/me")
    public MeResponse me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Teacher teacher = authService.getAuthenticatedTeacher(authHeader);
        return MeResponse.ofTeacher(teacher);
    }
}
