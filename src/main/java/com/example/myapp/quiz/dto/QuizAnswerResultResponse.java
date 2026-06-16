package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizAnswerResultResponse {
    private Long quizQuestionId;
    private String questionText;
    private String answerText;
    private String correctAnswer;
    private Boolean isCorrect;
    private String explanation;
}