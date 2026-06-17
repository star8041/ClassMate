package com.example.myapp.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 인증 관련 페이지 라우트.
 *
 * GET /login    로그인 페이지
 * GET /signup   회원가입 페이지
 */
@Controller
public class AuthPageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }
}
