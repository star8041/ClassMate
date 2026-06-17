package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudentPageController {

    @GetMapping({"/student/quiz", "/student/quiz/"})
    public String studentQuizPage() {
        return "quiz_student";
    }

    @GetMapping({"/student/quiz/solve", "/student/quiz/solve/"})
    public String studentQuizSolvePage(
            @RequestParam("quizId") Long quizId
    ) {
        return "quiz_student_solve";
    }
}

