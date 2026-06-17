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
        System.out.println("######## PageController dashboard 호출");
        return "teacher/dashboard";
    }

    @GetMapping("/teacher/archive")
    public String archive(Model model) {
        model.addAttribute("userName", null);
        model.addAttribute("profileImgUrl", null);
        return "teacher/archive";
    }
    
    @GetMapping("/quiz")
    public String quiz(Model model) {
    	model.addAttribute("userName", null);
    	model.addAttribute("profileImgUrl", null);
        return "quiz";
    }
    
    @GetMapping("/student")
    public String student(Model model) {
    	model.addAttribute("userName", null);
    	model.addAttribute("profileImgUrl", null);
        return "student";
    }
    
    @GetMapping("/counseling")
    public String counseling(Model model) {
    	model.addAttribute("userName", null);
    	model.addAttribute("profileImgUrl", null);
        return "counseling";
    }
    
    @GetMapping("/schedule")
    public String schedule(Model model) {
    	model.addAttribute("userName", null);
    	model.addAttribute("profileImgUrl", null);
        return "schedule";
    }
}
