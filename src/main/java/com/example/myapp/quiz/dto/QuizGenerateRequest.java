package com.example.myapp.quiz.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class QuizGenerateRequest {
    private Long teacherId;
    private Long materialId;

    private String title;
    private String difficulty;
    private Integer startPage;
    private Integer endPage;
    
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
}