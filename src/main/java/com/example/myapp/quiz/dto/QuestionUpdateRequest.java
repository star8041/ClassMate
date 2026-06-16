package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionUpdateRequest {
    private String questionType;
    private String questionText;

    // JSONB를 일단 문자열로 처리
    private String options;

    private String answerText;
    private String explanation;
    private Integer questionOrder;
}