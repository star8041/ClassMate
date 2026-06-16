package com.example.myapp.quiz.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizQuestion {
    private Long quizQuestionId;
    private Long quizId;
    private String questionType;
    private String questionText;
    private String options;
    private String answerText;
    private String explanation;
    private Integer questionOrder;
}