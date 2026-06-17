package com.example.myapp.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentQuizSummaryResponse {
	private Integer submittedQuizCount;
	private Integer totalQuizCount;
	private Double averageScore;
	private Double quizAttemptRate;
	private Integer aiCommentCount;
	private Integer score90Count;
	private Integer score80Count;
	private Integer score70Count;
	private Integer score69Count;
}