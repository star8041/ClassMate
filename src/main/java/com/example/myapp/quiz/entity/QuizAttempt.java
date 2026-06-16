package com.example.myapp.quiz.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * quiz_attempt 테이블 매핑 POJO.
 * quizTitle 은 quiz 테이블 JOIN 시 채워지는 필드 (DB 컬럼 아님).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {
    private Long quizAttemptId;
    private Long quizId;
    private String quizTitle;       // JOIN quiz.title
    private Long studentId;
    private Integer score;
    private Integer totalCount;
    private Integer correctCount;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
