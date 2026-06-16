package com.example.myapp.student.controller;

import com.example.myapp.student.dto.StudentCreateRequest;
import com.example.myapp.student.dto.StudentResponse;
import com.example.myapp.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 교사(me) 기준 학생 등록 API.
 *
 * <pre>
 * POST /api/v1/teachers/me/students   학생 등록
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/teachers/me/students")
public class TeacherStudentController {

    private final StudentService studentService;

    public TeacherStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * 학생 등록.
     * <p>
     * 현재 교사(me)는 원래 인증 정보(SecurityContext)에서 가져와야 하지만,
     * 인증 연동 전까지는 임시로 {@code X-Teacher-Id} 헤더로 받는다.
     * TODO: 인증 연동 후 헤더 대신 SecurityContext 의 교사 식별자로 대체할 것.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(
            @RequestHeader("X-Teacher-Id") Long teacherId,
            @RequestBody StudentCreateRequest request) {
        return studentService.create(teacherId, request);
    }
}
