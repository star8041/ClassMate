package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizQuestionRequest {
    private String questionType;
    private String questionText;
    private String options;
    private String answerText;
    private String explanation;
    private Integer questionOrder;
}