package com.example.myapp.quiz.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Quiz {
    private Long quizId;
    private Long teacherId;
    private Long materialId;
    private String title;
    private String difficulty;
    private Integer startPage;
    private Integer endPage;
    private LocalDateTime createdAt;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
}