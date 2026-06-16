package com.example.myapp.test;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/test")
    public String test(Model model) {
        model.addAttribute("userName", "홍길동");
        return "test";
    }
    
    @GetMapping("/archive")
    public String archive(Model model) {
        return "archive";
    }
}