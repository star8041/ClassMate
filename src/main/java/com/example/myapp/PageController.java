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
        model.addAttribute("userName", null);
        model.addAttribute("profileImgUrl", null);
        return "teacher/dashboard";
    }

    @GetMapping("/teacher/archive")
    public String archive(Model model) {
        model.addAttribute("userName", null);
        model.addAttribute("profileImgUrl", null);
        return "teacher/archive";
    }
}
