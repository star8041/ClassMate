package com.example.myapp.quiz.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAttemptResponse {
    private Long quizAttemptId;
    private Long quizId;
    private Long studentId;
    private Integer score;
    private Integer totalCount;
    private Integer correctCount;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}