package com.example.myapp.student.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudentPageController {

    @GetMapping({"/student/materials", "/student/materials/"})
    public String studentMaterialsPage() {
        return "student-materials";
    }

    @GetMapping({"/student/quiz", "/student/quiz/"})
    public String studentQuizPage() {
        return "student-quiz";
    }

    @GetMapping({"/student/quiz/solve", "/student/quiz/solve/"})
    public String studentQuizSolvePage(
            @RequestParam("quizId") Long quizId
    ) {
        return "student-quiz-solve";
    }
}

