package com.example.myapp.teacher.controller;

import com.example.myapp.teacher.dto.TeacherResponse;
import com.example.myapp.teacher.dto.TeacherSignupRequest;
import com.example.myapp.teacher.service.TeacherService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 교사 계정 API.
 *
 * <pre>
 * POST /api/v1/teachers/signup   선생님 회원가입
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** 선생님 회원가입 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherResponse signup(@RequestBody TeacherSignupRequest request) {
        return teacherService.signup(request);
    }
}
