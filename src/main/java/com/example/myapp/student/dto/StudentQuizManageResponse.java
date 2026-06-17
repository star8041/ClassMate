package com.example.myapp.student.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentQuizManageResponse {
	private Long studentId;
	private String studentName;
	private String studentNumber;
	private List<StudentQuizResultResponse> quizResults;
	private StudentQuizSummaryResponse summary;
}