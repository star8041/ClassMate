package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizQuestionResponse {
    private Long quizQuestionId;
    private Long quizId;
    private String questionType;
    private String questionText;

    // JSONB를 일단 문자열로 받음
    private String options;

    private String answerText;
    private String explanation;
    private Integer questionOrder;
}