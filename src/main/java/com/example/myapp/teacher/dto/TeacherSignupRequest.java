package com.example.myapp.teacher.dto;

/**
 * 선생님 회원가입 요청 DTO.
 * <p>
 * question/answer 는 teacher 테이블에서 NOT NULL 이라 받되, 미입력 시 빈 문자열로 저장한다.
 * (PDF 회원가입 화면엔 없지만 DDL 제약을 만족시키기 위함)
 */
public record TeacherSignupRequest(
        String teacherName,
        String loginId,
        String email,
        String password,
        String passwordConfirm,
        String question,
        String answer
) {
}
