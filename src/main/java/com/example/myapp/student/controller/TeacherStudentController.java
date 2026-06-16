package com.example.myapp.student.controller;

import com.example.myapp.student.dto.StudentCreateRequest;
import com.example.myapp.student.dto.StudentResponse;
import com.example.myapp.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 현재 교사(me) 기준 학생 컬렉션 API.
 *
 * <pre>
 * GET  /api/v1/teachers/me/students   내 학생 목록 조회
 * POST /api/v1/teachers/me/students   학생 등록
 * </pre>
 *
 * 현재 교사(me)는 원래 인증 정보(SecurityContext)에서 가져와야 하지만,
 * 인증 연동 전까지는 임시로 {@code X-Teacher-Id} 헤더로 받는다.
 * TODO: 인증 연동 후 헤더 대신 SecurityContext 의 교사 식별자로 대체할 것.
 */
@RestController
@RequestMapping("/api/v1/teachers/me/students")
public class TeacherStudentController {

    private final StudentService studentService;

    public TeacherStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** 내 학생 목록 조회 */
    @GetMapping
    public List<StudentResponse> list(@RequestHeader("X-Teacher-Id") Long teacherId) {
        return studentService.list(teacherId);
    }

    /** 학생 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestHeader("X-Teacher-Id") Long teacherId,
                                  @RequestBody StudentCreateRequest request) {
        return studentService.create(teacherId, request);
    }
}
