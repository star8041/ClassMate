package com.example.myapp.quiz.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswer {
    private Long quizAnswerId;
    private Long quizAttemptId;
    private Long quizQuestionId;
    private String answerText;
    private Boolean isCorrect;
}