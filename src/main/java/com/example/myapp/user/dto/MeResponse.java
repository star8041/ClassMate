package com.example.myapp.user.dto;

import com.example.myapp.teacher.entity.Teacher;

/**
 * 내 정보 조회 응답 DTO.
 */
public record MeResponse(
        Long userId,
        String loginId,
        String name,
        String email,
        String role
) {
    public static MeResponse ofTeacher(Teacher teacher) {
        return new MeResponse(
                teacher.getTeacherId(),
                teacher.getLoginId(),
                teacher.getTeacherName(),
                teacher.getEmail(),
                "TEACHER"
        );
    }
}
