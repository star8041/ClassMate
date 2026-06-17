package com.example.myapp.student.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.myapp.student.dto.StudentListResponse;
import com.example.myapp.student.dto.StudentQuizManageResponse;
import com.example.myapp.student.service.StudentManageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teacher/students")
@RequiredArgsConstructor
public class StudentManageController {

    private final StudentManageService studentManageService;

    @GetMapping
    public List<StudentListResponse> getMyStudents(
            @AuthenticationPrincipal Long teacherId
    ) {
        if (teacherId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return studentManageService.getStudentsByTeacherId(teacherId);
    }

    @GetMapping("/{studentId}/quiz-results")
    public StudentQuizManageResponse getStudentQuizResults(
            @PathVariable("studentId") Long studentId,
            @AuthenticationPrincipal Long teacherId
    ) {
        if (teacherId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return studentManageService.getStudentQuizResults(teacherId, studentId);
    }
}