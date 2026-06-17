package com.example.myapp.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentListResponse {
	private Long studentId;
	private String studentName;
	private String studentNumber;
}