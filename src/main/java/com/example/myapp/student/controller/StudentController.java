package com.example.myapp.student.controller;

import com.example.myapp.student.dto.StudentResponse;
import com.example.myapp.student.dto.StudentUpdateRequest;
import com.example.myapp.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 학생 관리 REST API.
 *
 * <pre>
 * GET    /api/students            학생 목록 조회
 * GET    /api/students/{id}       학생 상세 조회
 * PUT    /api/students/{id}       학생 정보 수정
 * POST   /api/students/{id}/delete 학생 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** 학생 목록 조회 (teacherId 미지정 시 전체) */
    @GetMapping
    public List<StudentResponse> list(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return studentService.list(teacherId);
    }

    /** 학생 상세 조회 */
    @GetMapping("/{id}")
    public StudentResponse detail(@PathVariable("id") Long id) {
        return studentService.get(id);
    }

    /** 학생 정보 수정 */
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable("id") Long id,
                                  @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    /** 학생 삭제 (material 도메인과 동일하게 POST 사용) */
    @PostMapping("/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        studentService.delete(id);
    }
}
