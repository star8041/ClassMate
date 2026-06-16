package com.example.myapp.teacher.controller;

import com.example.myapp.auth.service.AuthService;
import com.example.myapp.teacher.dto.TeacherResponse;
import com.example.myapp.teacher.service.TeacherService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관리자(ADMIN) 전용 API.
 *
 * <pre>
 * GET /api/v1/admin/teachers   전체 교사(사용자) 목록 조회 — ROLE_ADMIN 만 허용
 * </pre>
 *
 * 인가는 JWT 의 role 클레임 기반으로 컨트롤러에서 검증한다(AuthService.requireAdmin).
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuthService authService;
    private final TeacherService teacherService;

    public AdminController(AuthService authService, TeacherService teacherService) {
        this.authService = authService;
        this.teacherService = teacherService;
    }

    /** 전체 교사 목록 조회 (관리자 전용) */
    @GetMapping("/teachers")
    public List<TeacherResponse> listTeachers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        authService.requireAdmin(authHeader); // 관리자 아니면 403
        return teacherService.listAll();
    }
}
