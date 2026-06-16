package com.example.myapp.student.dto;

/**
 * 학생 등록 요청 DTO.
 * 담당 교사(teacherId)는 본문이 아니라 "현재 교사(me)"에서 결정된다.
 */
public record StudentCreateRequest(
        String studentName,
        String studentNumber
) {
}
