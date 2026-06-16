package com.example.myapp.student.controller;

import com.example.myapp.student.dto.StudentResponse;
import com.example.myapp.student.dto.StudentUpdateRequest;
import com.example.myapp.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생 단건 리소스 API.
 *
 * <pre>
 * GET    /api/v1/students/{studentId}   학생 상세 조회
 * PATCH  /api/v1/students/{studentId}   학생 정보 수정 (부분 수정)
 * DELETE /api/v1/students/{studentId}   학생 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** 학생 상세 조회 */
    @GetMapping("/{studentId}")
    public StudentResponse detail(@PathVariable("studentId") Long studentId) {
        return studentService.get(studentId);
    }

    /** 학생 정보 수정 (전달된 필드만 변경) */
    @PatchMapping("/{studentId}")
    public StudentResponse update(@PathVariable("studentId") Long studentId,
                                  @RequestBody StudentUpdateRequest request) {
        return studentService.update(studentId, request);
    }

    /** 학생 삭제 */
    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("studentId") Long studentId) {
        studentService.delete(studentId);
    }
}
