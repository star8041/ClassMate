package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 학생용 페이지 라우트.
 *
 * <pre>
 * GET /student/quiz        퀴즈 목록
 * GET /student/quiz/solve  퀴즈 풀이
 * GET /student/materials   강의자료
 * </pre>
 *
 * (학생 홈은 StudentHomePageController 의 /student/home)
 */
@Controller
public class StudentPageController {

    @GetMapping("/student/quiz")
    public String studentQuiz() {
        return "student-quiz";
    }

    @GetMapping("/student/quiz/solve")
    public String studentQuizSolve() {
        return "student-quiz-solve";
    }

    @GetMapping("/student/materials")
    public String studentMaterials() {
        return "student-materials";
    }
}
