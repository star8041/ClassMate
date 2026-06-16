package com.example.myapp.quiz.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.myapp.quiz.dto.QuizDetailResponse;
import com.example.myapp.quiz.dto.QuizGenerateRequest;
import com.example.myapp.quiz.dto.QuizListResponse;
import com.example.myapp.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import com.example.myapp.quiz.dto.QuestionUpdateRequest;
import com.example.myapp.quiz.dto.QuizUpdateRequest;
import com.example.myapp.quiz.dto.QuizAttemptResponse;
import com.example.myapp.quiz.dto.QuizAttemptResultResponse;
import com.example.myapp.quiz.dto.QuizAttemptStartRequest;
import com.example.myapp.quiz.dto.QuizSubmitRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public QuizDetailResponse generateQuiz(@RequestBody QuizGenerateRequest request) {
        return quizService.generateQuiz(request);
    }

    @GetMapping
    public List<QuizListResponse> getQuizList() {
        return quizService.getQuizList();
    }

    @GetMapping("/{quizId}")
    public QuizDetailResponse getQuiz(@PathVariable("quizId") Long quizId) {
        return quizService.getQuiz(quizId);
    }
    
    @PatchMapping("/{quizId}")
    public QuizDetailResponse updateQuiz(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizUpdateRequest request
    ) {
        return quizService.updateQuiz(quizId, request);
    }

    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> deleteQuiz(
            @PathVariable("quizId") Long quizId
    ) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{quizId}/questions/{questionId}")
    public QuizDetailResponse updateQuestion(
            @PathVariable("quizId") Long quizId,
            @PathVariable("questionId") Long questionId,
            @RequestBody QuestionUpdateRequest request
    ) {
        return quizService.updateQuestion(quizId, questionId, request);
    }
    
    @PostMapping("/{quizId}/attempts")
    public QuizAttemptResponse startAttempt(
            @PathVariable("quizId") Long quizId,
            @RequestBody QuizAttemptStartRequest request
    ) {
        return quizService.startAttempt(quizId, request);
    }

    @PostMapping("/{quizId}/attempts/{attemptId}/submit")
    public QuizAttemptResultResponse submitQuiz(
            @PathVariable("quizId") Long quizId,
            @PathVariable("attemptId") Long attemptId,
            @RequestBody QuizSubmitRequest request
    ) {
        return quizService.submitQuiz(quizId, attemptId, request);
    }

    @GetMapping("/{quizId}/attempts/{attemptId}")
    public QuizAttemptResultResponse getAttemptResult(
            @PathVariable("quizId") Long quizId,
            @PathVariable("attemptId") Long attemptId
    ) {
        return quizService.getAttemptResult(quizId, attemptId);
    }
}