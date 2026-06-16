package com.example.myapp.quiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.domain.QuizAnswer;
import com.example.myapp.quiz.domain.QuizAttempt;
import com.example.myapp.quiz.domain.QuizQuestion;
import com.example.myapp.quiz.dto.QuestionUpdateRequest;
import com.example.myapp.quiz.dto.QuizAnswerResultResponse;
import com.example.myapp.quiz.dto.QuizAttemptResponse;
import com.example.myapp.quiz.dto.QuizAttemptResultResponse;
import com.example.myapp.quiz.dto.QuizAttemptStartRequest;
import com.example.myapp.quiz.dto.QuizDetailResponse;
import com.example.myapp.quiz.dto.QuizGenerateRequest;
import com.example.myapp.quiz.dto.QuizListResponse;
import com.example.myapp.quiz.dto.QuizQuestionResponse;
import com.example.myapp.quiz.dto.QuizSubmitRequest;
import com.example.myapp.quiz.dto.QuizUpdateRequest;
import com.example.myapp.quiz.mapper.QuizMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizMapper quizMapper;

    @Transactional
    public QuizDetailResponse generateQuiz(QuizGenerateRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTeacherId(request.getTeacherId());
        quiz.setMaterialId(request.getMaterialId());
        quiz.setTitle(request.getTitle());
        quiz.setDifficulty(request.getDifficulty());
        quiz.setStartPage(request.getStartPage());
        quiz.setEndPage(request.getEndPage());
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());

        quizMapper.insertQuiz(quiz);

        List<QuizQuestion> questions = List.of(
                createDummyQuestion(
                        quiz.getQuizId(),
                        "MULTIPLE_CHOICE",
                        "스택의 특징으로 알맞은 것은?",
                        """
                        [
                          {"number": 1, "text": "선입선출 구조"},
                          {"number": 2, "text": "후입선출 구조"},
                          {"number": 3, "text": "무작위 접근 구조"},
                          {"number": 4, "text": "정렬 구조"}
                        ]
                        """,
                        "2",
                        "스택은 마지막에 들어온 데이터가 먼저 나가는 후입선출 구조입니다.",
                        1
                ),
                createDummyQuestion(
                        quiz.getQuizId(),
                        "MULTIPLE_CHOICE",
                        "큐의 특징으로 알맞은 것은?",
                        """
                        [
                          {"number": 1, "text": "후입선출 구조"},
                          {"number": 2, "text": "선입선출 구조"},
                          {"number": 3, "text": "트리 구조"},
                          {"number": 4, "text": "그래프 구조"}
                        ]
                        """,
                        "2",
                        "큐는 먼저 들어온 데이터가 먼저 나가는 선입선출 구조입니다.",
                        2
                ),
                createDummyQuestion(
                        quiz.getQuizId(),
                        "MULTIPLE_CHOICE",
                        "스택에서 데이터를 꺼내는 연산은?",
                        """
                        [
                          {"number": 1, "text": "push"},
                          {"number": 2, "text": "pop"},
                          {"number": 3, "text": "enqueue"},
                          {"number": 4, "text": "dequeue"}
                        ]
                        """,
                        "2",
                        "스택에서 데이터를 꺼내는 연산은 pop입니다.",
                        3
                )
        );

        for (QuizQuestion question : questions) {
            quizMapper.insertQuizQuestion(question);
        }

        return getQuiz(quiz.getQuizId());
    }

    public List<QuizListResponse> getQuizList() {
        return quizMapper.findQuizList();
    }

    public QuizDetailResponse getQuiz(Long quizId) {
        Quiz quiz = quizMapper.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        List<QuizQuestion> questions = quizMapper.findQuestionsByQuizId(quizId);

        QuizDetailResponse response = new QuizDetailResponse();
        response.setQuizId(quiz.getQuizId());
        response.setTeacherId(quiz.getTeacherId());
        response.setMaterialId(quiz.getMaterialId());
        response.setTitle(quiz.getTitle());
        response.setDifficulty(quiz.getDifficulty());
        response.setStartPage(quiz.getStartPage());
        response.setEndPage(quiz.getEndPage());
        response.setCreatedAt(quiz.getCreatedAt());

        response.setQuestions(
                questions.stream()
                        .map(this::toQuestionResponse)
                        .toList()
        );

        return response;
    }

    @Transactional
    public QuizDetailResponse updateQuiz(Long quizId, QuizUpdateRequest request) {
        int updatedCount = quizMapper.updateQuiz(quizId, request);

        if (updatedCount == 0) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        return getQuiz(quizId);
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        int deletedCount = quizMapper.deleteQuiz(quizId);

        if (deletedCount == 0) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }
    }

    @Transactional
    public QuizDetailResponse updateQuestion(
            Long quizId,
            Long questionId,
            QuestionUpdateRequest request
    ) {
        int updatedCount = quizMapper.updateQuestion(quizId, questionId, request);

        if (updatedCount == 0) {
            throw new IllegalArgumentException(
                    "존재하지 않는 문제입니다. quizId=" + quizId + ", questionId=" + questionId
            );
        }

        return getQuiz(quizId);
    }

    @Transactional
    public QuizAttemptResponse startAttempt(Long quizId, QuizAttemptStartRequest request) {
        Quiz quiz = quizMapper.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        validateQuizAvailable(quiz);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setStudentId(request.getStudentId());

        quizMapper.insertQuizAttempt(attempt);

        QuizAttempt savedAttempt = quizMapper.findAttemptById(quizId, attempt.getQuizAttemptId());
        return toAttemptResponse(savedAttempt);
    }

    @Transactional
    public QuizAttemptResultResponse submitQuiz(
            Long quizId,
            Long attemptId,
            QuizSubmitRequest request
    ) {
        Quiz quiz = quizMapper.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        validateSubmitAvailable(quiz);

        QuizAttempt attempt = quizMapper.findAttemptById(quizId, attemptId);

        if (attempt == null) {
            throw new IllegalArgumentException("존재하지 않는 응시 기록입니다. attemptId=" + attemptId);
        }

        if (attempt.getSubmittedAt() != null) {
            throw new IllegalArgumentException("이미 제출된 퀴즈입니다. attemptId=" + attemptId);
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("제출된 답안이 없습니다.");
        }

        List<QuizQuestion> questions = quizMapper.findQuestionsByQuizId(quizId);

        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        QuizQuestion::getQuizQuestionId,
                        Function.identity()
                ));

        int totalCount = request.getAnswers().size();
        int correctCount = 0;

        for (var submittedAnswer : request.getAnswers()) {
            QuizQuestion question = questionMap.get(submittedAnswer.getQuizQuestionId());

            if (question == null) {
                throw new IllegalArgumentException(
                        "퀴즈에 포함되지 않은 문제입니다. questionId=" + submittedAnswer.getQuizQuestionId()
                );
            }

            boolean isCorrect = normalize(question.getAnswerText())
                    .equals(normalize(submittedAnswer.getAnswerText()));

            if (isCorrect) {
                correctCount++;
            }

            QuizAnswer answer = new QuizAnswer();
            answer.setQuizAttemptId(attemptId);
            answer.setQuizQuestionId(submittedAnswer.getQuizQuestionId());
            answer.setAnswerText(submittedAnswer.getAnswerText());
            answer.setIsCorrect(isCorrect);

            quizMapper.insertQuizAnswer(answer);
        }

        int score = totalCount == 0
                ? 0
                : (int) Math.round((correctCount * 100.0) / totalCount);

        quizMapper.updateAttemptResult(
                attemptId,
                score,
                totalCount,
                correctCount
        );

        return getAttemptResult(quizId, attemptId);
    }

    public QuizAttemptResultResponse getAttemptResult(Long quizId, Long attemptId) {
        QuizAttempt attempt = quizMapper.findAttemptById(quizId, attemptId);

        if (attempt == null) {
            throw new IllegalArgumentException("존재하지 않는 응시 기록입니다. attemptId=" + attemptId);
        }

        List<QuizAnswerResultResponse> answers =
                quizMapper.findAnswerResultsByAttemptId(attemptId);

        QuizAttemptResultResponse response = new QuizAttemptResultResponse();
        response.setQuizAttemptId(attempt.getQuizAttemptId());
        response.setQuizId(attempt.getQuizId());
        response.setStudentId(attempt.getStudentId());
        response.setScore(attempt.getScore());
        response.setTotalCount(attempt.getTotalCount());
        response.setCorrectCount(attempt.getCorrectCount());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        response.setAnswers(answers);

        return response;
    }

    private QuizQuestion createDummyQuestion(
            Long quizId,
            String questionType,
            String questionText,
            String options,
            String answerText,
            String explanation,
            Integer questionOrder
    ) {
        QuizQuestion question = new QuizQuestion();
        question.setQuizId(quizId);
        question.setQuestionType(questionType);
        question.setQuestionText(questionText);
        question.setOptions(options);
        question.setAnswerText(answerText);
        question.setExplanation(explanation);
        question.setQuestionOrder(questionOrder);
        return question;
    }

    private QuizQuestionResponse toQuestionResponse(QuizQuestion question) {
        QuizQuestionResponse response = new QuizQuestionResponse();
        response.setQuizQuestionId(question.getQuizQuestionId());
        response.setQuizId(question.getQuizId());
        response.setQuestionType(question.getQuestionType());
        response.setQuestionText(question.getQuestionText());
        response.setOptions(question.getOptions());
        response.setAnswerText(question.getAnswerText());
        response.setExplanation(question.getExplanation());
        response.setQuestionOrder(question.getQuestionOrder());
        return response;
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt) {
        QuizAttemptResponse response = new QuizAttemptResponse();
        response.setQuizAttemptId(attempt.getQuizAttemptId());
        response.setQuizId(attempt.getQuizId());
        response.setStudentId(attempt.getStudentId());
        response.setScore(attempt.getScore());
        response.setTotalCount(attempt.getTotalCount());
        response.setCorrectCount(attempt.getCorrectCount());
        response.setStartedAt(attempt.getStartedAt());
        response.setSubmittedAt(attempt.getSubmittedAt());
        return response;
    }

    private void validateQuizAvailable(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();

        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            throw new IllegalArgumentException("아직 응시 가능한 시간이 아닙니다.");
        }

        if (quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil())) {
            throw new IllegalArgumentException("퀴즈 응시 가능 시간이 지났습니다.");
        }
    }

    private void validateSubmitAvailable(Quiz quiz) {
        LocalDateTime now = LocalDateTime.now();

        if (quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil())) {
            throw new IllegalArgumentException("제출 마감 시간이 지났습니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}