package com.example.myapp.quiz.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * quiz_answer 테이블 매핑 POJO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswer {
    private Long quizAnswerId;
    private Long quizAttemptId;
    private Long quizQuestionId;
    private String answerText;
    private Boolean isCorrect;
}
