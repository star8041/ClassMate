package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizUpdateRequest {
    private String title;
    private String difficulty;
    private Integer startPage;
    private Integer endPage;
}