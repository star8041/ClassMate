package com.example.myapp.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회원 관련 페이지 라우트.
 *
 * GET /members/logout   로그아웃 (클라이언트 토큰 정리 후 /login 으로 이동)
 */
@Controller
public class MemberPageController {

    @GetMapping("/members/logout")
    public String logout() {
        return "logout";
    }
}
