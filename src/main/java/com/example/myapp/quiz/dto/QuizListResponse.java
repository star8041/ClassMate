package com.example.myapp.quiz.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizListResponse {

    private Long quizId;
    private Long teacherId;
    private Long materialId;
    private String title;
    private String difficulty;
    private Integer startPage;
    private Integer endPage;

    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;

    private LocalDateTime createdAt;
    private Integer questionCount;
}