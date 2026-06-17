package com.example.myapp.student.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.myapp.student.dto.StudentListResponse;
import com.example.myapp.student.dto.StudentQuizManageResponse;
import com.example.myapp.student.dto.StudentQuizResultResponse;
import com.example.myapp.student.dto.StudentQuizSummaryResponse;
import com.example.myapp.student.repository.StudentManageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentManageService {

    private final StudentManageRepository studentManageRepository;

    public List<StudentListResponse> getStudentsByTeacherId(Long teacherId) {
        return studentManageRepository.findStudentsByTeacherId(teacherId);
    }

    public StudentQuizManageResponse getStudentQuizResults(Long teacherId, Long studentId) {
        StudentListResponse student = studentManageRepository.findStudentByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("담당 학생이 아니거나 존재하지 않는 학생입니다. studentId=" + studentId));

        List<StudentQuizResultResponse> quizResults =
                studentManageRepository.findQuizResultsByStudentId(studentId, teacherId);

        int totalQuizCount = studentManageRepository.countTeacherQuiz(teacherId);
        StudentQuizSummaryResponse summary = createSummary(quizResults, totalQuizCount);

        StudentQuizManageResponse response = new StudentQuizManageResponse();

        response.setStudentId(student.getStudentId());
        response.setStudentName(student.getStudentName());
        response.setStudentNumber(student.getStudentNumber());
        response.setQuizResults(quizResults);
        response.setSummary(summary);

        return response;
    }

    private StudentQuizSummaryResponse createSummary(
            List<StudentQuizResultResponse> quizResults,
            int totalQuizCount
    ) {
        StudentQuizSummaryResponse summary = new StudentQuizSummaryResponse();

        int submittedQuizCount = quizResults.size();

        double averageScore = quizResults.stream()
                .filter(result -> result.getScore() != null)
                .mapToInt(StudentQuizResultResponse::getScore)
                .average()
                .orElse(0.0);

        double quizAttemptRate = totalQuizCount == 0
                ? 0.0
                : Math.round((submittedQuizCount * 1000.0) / totalQuizCount) / 10.0;

        int score90Count = 0;
        int score80Count = 0;
        int score70Count = 0;
        int score69Count = 0;

        for (StudentQuizResultResponse result : quizResults) {
            int score = result.getScore() == null ? 0 : result.getScore();

            if (score >= 90) {
                score90Count++;
            } else if (score >= 80) {
                score80Count++;
            } else if (score >= 70) {
                score70Count++;
            } else {
                score69Count++;
            }
        }

        summary.setSubmittedQuizCount(submittedQuizCount);
        summary.setTotalQuizCount(totalQuizCount);
        summary.setAverageScore(Math.round(averageScore * 10.0) / 10.0);
        summary.setQuizAttemptRate(quizAttemptRate);

        // AI 코멘트는 아직 자동 생성 전이므로 0으로 둔다.
        summary.setAiCommentCount(0);

        summary.setScore90Count(score90Count);
        summary.setScore80Count(score80Count);
        summary.setScore70Count(score70Count);
        summary.setScore69Count(score69Count);

        return summary;
    }
}