package com.example.myapp.teacher.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자(ADMIN) 로그인 후 진입 화면 선택 페이지.
 *
 * GET /admin/select   선생님 화면 / 학생 화면 선택
 */
@Controller
public class AdminSelectPageController {

    @GetMapping("/admin/select")
    public String adminSelect() {
        return "admin-select";
    }
}
