package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswerSubmitRequest {
    private Long quizQuestionId;
    private String answerText;
}