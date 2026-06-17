package com.example.myapp.student.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam("teacherId") Long teacherId
    ) {
        return studentManageService.getStudentsByTeacherId(teacherId);
    }

    @GetMapping("/{studentId}/quiz-results")
    public StudentQuizManageResponse getStudentQuizResults(
            @PathVariable("studentId") Long studentId,
            @RequestParam("teacherId") Long teacherId
    ) {
        return studentManageService.getStudentQuizResults(teacherId, studentId);
    }
}