package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping({"/", "/teacher/dashboard"})
    public String dashboard(Model model) {
        // userName/profileImgUrl은 JWT 필터 연동 전까지 null — JS에서 토큰 파싱 후 업데이트
        model.addAttribute("userName", null);
        model.addAttribute("profileImgUrl", null);
        return "teacher/dashboard";
    }
}
