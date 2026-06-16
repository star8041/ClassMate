package com.example.myapp.teacher.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 교사 계정 엔티티. teacher 테이블과 1:1 매핑된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {

    private Long teacherId;
    private String loginId;
    private String password;       // BCrypt 해시 저장
    private String teacherName;
    private String email;
    private String question;       // 비밀번호 찾기용 질문
    private String answer;         // 비밀번호 찾기용 답
    private String role;           // USER(교사) / ADMIN(관리자)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
