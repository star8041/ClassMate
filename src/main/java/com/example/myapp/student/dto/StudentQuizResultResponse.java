package com.example.myapp.student.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentQuizResultResponse {
	private Long quizId;
	private Long attemptId;
	private String quizTitle;
	private LocalDateTime submittedAt;
	private Integer score;
	private Integer totalCount;
	private Integer correctCount;
	private String comment;
}