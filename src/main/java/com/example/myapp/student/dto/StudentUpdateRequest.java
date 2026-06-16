package com.example.myapp.student.dto;

/**
 * 학생 정보 수정 요청 DTO.
 * (담당 교사 변경은 허용하지 않고 이름/학번만 수정한다.)
 */
public record StudentUpdateRequest(
        String studentName,
        String studentNumber
) {
}
