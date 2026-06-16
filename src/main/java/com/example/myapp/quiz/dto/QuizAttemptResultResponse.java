package com.example.myapp.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAttemptResultResponse {
    private Long quizAttemptId;
    private Long quizId;
    private Long studentId;
    private Integer score;
    private Integer totalCount;
    private Integer correctCount;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private List<QuizAnswerResultResponse> answers;
}