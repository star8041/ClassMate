package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 학생용 홈 페이지 라우트.
 *
 * <pre>
 * GET /student/home   학생 홈 (AI 질문 · 오늘 시간표 · 오늘 할 일 · 공지사항)
 * </pre>
 */
@Controller
public class StudentHomePageController {

    @GetMapping("/student/home")
    public String studentHome() {
        return "student-home";
    }
}
