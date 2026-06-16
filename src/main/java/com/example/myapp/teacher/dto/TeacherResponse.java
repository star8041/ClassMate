package com.example.myapp.teacher.dto;

import com.example.myapp.teacher.entity.Teacher;

/**
 * 교사 정보 응답 DTO. (비밀번호 등 민감 정보는 제외)
 */
public record TeacherResponse(
        Long teacherId,
        String loginId,
        String teacherName,
        String email,
        String role
) {
    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getTeacherId(),
                teacher.getLoginId(),
                teacher.getTeacherName(),
                teacher.getEmail(),
                teacher.getRole()
        );
    }
}
