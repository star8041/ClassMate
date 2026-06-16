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
    	model.addAttribute("userName", "홍길동");
        return "archive";
    }
    
    @GetMapping("/quiz")
    public String quiz(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "quiz";
    }
    
    @GetMapping("/quiz/student")
    public String quizStudent(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "quiz_student";
    }
    
    @GetMapping("/quiz/student/solve")
    public String quizStudentSolve(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "quiz_student_solve";
    }
    
    @GetMapping("/student")
    public String student(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "student";
    }
    
    @GetMapping("/counseling")
    public String counseling(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "counseling";
    }
    
    @GetMapping("/schedule")
    public String schedule(Model model) {
    	model.addAttribute("userName", "홍길동");
        return "schedule";
    }
}