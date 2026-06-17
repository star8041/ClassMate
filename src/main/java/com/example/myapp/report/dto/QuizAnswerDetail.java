package com.example.myapp.report.dto;

import java.time.LocalDateTime;

public record QuizAnswerDetail(
        Long quizAttemptId,
        Long quizId,
        String quizTitle,
        Integer score,
        Integer correctCount,
        Integer totalCount,
        LocalDateTime submittedAt,

        Long quizQuestionId,
        Integer questionOrder,
        String questionText,
        String options,
        String studentAnswer,
        String correctAnswer,
        String explanation,
        Boolean isCorrect
) {
}

