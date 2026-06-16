package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 관리자(ADMIN) 로그인 후 진입 화면 선택 페이지.
 *
 * <pre>
 * GET /admin/select   선생님 화면 / 학생 화면 선택
 * </pre>
 *
 * 관리자 계정으로 로그인하면 로그인 화면에서 이 경로로 보내고,
 * 여기서 교사용/학생용 화면 중 하나를 골라 진입한다.
 */
@Controller
public class AdminSelectPageController {

    @GetMapping("/admin/select")
    public String adminSelect() {
        return "admin-select";
    }
}
