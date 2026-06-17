package com.example.myapp.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 학생 지도(성취도) 리포트 페이지 라우트.
 *
 * <pre>
 * GET /student/report   학생 관리 - 성취도 리포트 화면
 * </pre>
 */
@Controller
public class ReportPageController {

    @GetMapping("/student/report")
    public String studentReport(Model model) {
        model.addAttribute("userName", null);
        model.addAttribute("profileImgUrl", null);
        return "report";
    }
}
