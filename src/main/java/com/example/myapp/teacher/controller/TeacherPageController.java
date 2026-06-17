package com.example.myapp.teacher.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 교사용 페이지 라우트.
 *
 * GET /                  대시보드 (리다이렉트)
 * GET /teacher/dashboard 대시보드
 * GET /teacher/archive   교재 아카이브
 * GET /quiz              퀴즈 관리
 * GET /student           학생 관리
 * GET /counseling        상담 기록
 * GET /schedule          일정 관리
 */
@Controller
public class TeacherPageController {

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
