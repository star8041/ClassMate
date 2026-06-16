package com.example.myapp.student.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 교사가 관리하는 학생 정보를 나타내는 엔티티.
 * student 테이블과 1:1 매핑된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    /** PK (BIGSERIAL) */
    private Long studentId;

    /** 담당 교사 ID (FK) */
    private Long teacherId;

    /** 학생 이름 */
    private String studentName;

    /** 학번 (선택) */
    private String studentNumber;

    /** 생성 시각 */
    private LocalDateTime createdAt;

    /** 수정 시각 */
    private LocalDateTime updatedAt;
}
