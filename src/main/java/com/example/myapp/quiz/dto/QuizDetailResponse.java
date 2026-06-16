package com.example.myapp.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizDetailResponse {

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

    private List<QuizQuestionResponse> questions;
}