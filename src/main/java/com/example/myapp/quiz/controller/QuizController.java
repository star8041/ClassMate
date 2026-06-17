package com.example.myapp.quiz.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.myapp.quiz.dto.QuestionUpdateRequest;
import com.example.myapp.quiz.dto.QuizAttemptResponse;
import com.example.myapp.quiz.dto.QuizAttemptResultResponse;
import com.example.myapp.quiz.dto.QuizAttemptStartRequest;
import com.example.myapp.quiz.dto.QuizDetailResponse;
import com.example.myapp.quiz.dto.QuizGenerateRequest;
import com.example.myapp.quiz.dto.QuizListResponse;
import com.example.myapp.quiz.dto.QuizSubmitRequest;
import com.example.myapp.quiz.dto.QuizUpdateRequest;
import com.example.myapp.quiz.service.QuizService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * 퀴즈 미리보기 생성
     * DB에 저장하지 않고 화면에 보여줄 문제만 반환한다.
     */
    @PostMapping("/preview")
    public QuizDetailResponse previewQuiz(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody QuizGenerateRequest request
    ) {
        request.setTeacherId(resolveTeacherId(teacherId, request));
        return quizService.previewQuiz(request);
    }

    /**
     * 학생 배포
     * 미리보기에서 수정한 문제들을 DB에 저장한다.
     * 현재 구조에서는 DB 저장 = 학생에게 배포된 퀴즈로 본다.
     */
    @PostMapping("/generate")
    public QuizDetailResponse generateQuiz(
            @AuthenticationPrincipal Long teacherId,
            @RequestBody QuizGenerateRequest request
    ) {
        request.setTeacherId(resolveTeacherId(teacherId, request));
        return quizService.generateQuiz(request);
    }

    /**
     * 배포된 퀴즈 목록 조회
     */
    @GetMapping
    public List<QuizListResponse> getQuizList() {
        return quizService.getQuizList();
    }

    /**
     * 학생용 퀴즈 목록 조회
     * 학생이 이미 제출했는지 여부까지 함께 반환한다.
     */
    @GetMapping("/student")
    public List<QuizListResponse> getStudentQuizList(
            @RequestParam("studentId") Long studentId
    ) {
        return quizService.getStudentQuizList(studentId);
    }

    /**
     * 퀴즈 상세 조회
     */
    @GetMapping("/{quizId}")
    public QuizDetailResponse getQuiz(@PathVariable("quizId") Long quizId) {
        return quizService.getQuiz(quizId);
    }

    /**
     * 퀴즈 정보 수정
     */
    @PatchMapping("/{quizId}")
    public QuizDetailResponse updateQuiz(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizUpdateRequest request
    ) {
        return quizService.updateQuiz(quizId, request);
    }

    /**
     * 퀴즈 삭제
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable("quizId") Long quizId
    ) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 퀴즈 문제 수정
     */
    @PatchMapping("/{quizId}/questions/{questionId}")
    public QuizDetailResponse updateQuestion(
            @PathVariable("quizId") Long quizId,
            @PathVariable("questionId") Long questionId,
            @RequestBody QuestionUpdateRequest request
    ) {
        return quizService.updateQuestion(quizId, questionId, request);
    }

    /**
     * 학생 퀴즈 응시 시작
     */
    @PostMapping("/{quizId}/attempts")
    public QuizAttemptResponse startAttempt(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizAttemptStartRequest request
    ) {
        return quizService.startAttempt(quizId, request);
    }

    /**
     * 학생 퀴즈 제출
     */
    @PostMapping("/{quizId}/attempts/{attemptId}/submit")
    public QuizAttemptResultResponse submitQuiz(
            @PathVariable("quizId") Long quizId,
            @PathVariable("attemptId") Long attemptId,
            @RequestBody QuizSubmitRequest request
    ) {
        return quizService.submitQuiz(quizId, attemptId, request);
    }

    /**
     * 학생 퀴즈 결과 조회
     */
    @GetMapping("/{quizId}/attempts/{attemptId}")
    public QuizAttemptResultResponse getAttemptResult(
            @PathVariable("quizId") Long quizId,
            @PathVariable("attemptId") Long attemptId
    ) {
        return quizService.getAttemptResult(quizId, attemptId);
    }

    private Long resolveTeacherId(Long principalTeacherId, QuizGenerateRequest request) {
        if (principalTeacherId != null) {
            return principalTeacherId;
        }

        if (request.getTeacherId() != null) {
            return request.getTeacherId();
        }

        throw new IllegalArgumentException("로그인한 교사 정보를 확인할 수 없습니다.");
    }
}