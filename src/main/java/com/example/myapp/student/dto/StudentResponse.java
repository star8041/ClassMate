package com.example.myapp.student.dto;

import com.example.myapp.student.entity.Student;

import java.time.LocalDateTime;

/**
 * 학생 조회 응답 DTO.
 */
public record StudentResponse(
        Long studentId,
        Long teacherId,
        String studentName,
        String studentNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getStudentId(),
                student.getTeacherId(),
                student.getStudentName(),
                student.getStudentNumber(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
