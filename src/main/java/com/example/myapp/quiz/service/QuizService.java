package com.example.myapp.quiz.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.example.myapp.quiz.dto.QuizAnswerSubmitRequest;
import com.example.myapp.quiz.dto.QuizAttemptResponse;
import com.example.myapp.quiz.dto.QuizAttemptResultResponse;
import com.example.myapp.quiz.dto.QuizAttemptStartRequest;
import com.example.myapp.quiz.dto.QuizDetailResponse;
import com.example.myapp.quiz.dto.QuizGenerateRequest;
import com.example.myapp.quiz.dto.QuizListResponse;
import com.example.myapp.quiz.dto.QuizQuestionRequest;
import com.example.myapp.quiz.dto.QuizQuestionResponse;
import com.example.myapp.quiz.dto.QuizSubmitRequest;
import com.example.myapp.quiz.dto.QuizUpdateRequest;
import com.example.myapp.quiz.repository.QuizRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    /**
     * 퀴즈 미리보기 생성
     *
     * DB에 저장하지 않는다.
     * 현재는 AI 생성 기능이 없으므로 테스트용 더미 문제를 생성해서 반환한다.
     */
    public QuizDetailResponse previewQuiz(QuizGenerateRequest request) {
        QuizDetailResponse response = new QuizDetailResponse();

        response.setQuizId(null);
        response.setTeacherId(request.getTeacherId());
        response.setMaterialId(request.getMaterialId());
        response.setTitle(request.getTitle());
        response.setDifficulty(request.getDifficulty());
        response.setStartPage(request.getStartPage());
        response.setEndPage(request.getEndPage());
        response.setAvailableFrom(request.getAvailableFrom());
        response.setAvailableUntil(request.getAvailableUntil());
        response.setCreatedAt(LocalDateTime.now());

        List<QuizQuestionRequest> previewQuestions;

        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            previewQuestions = request.getQuestions();
        } else {
            previewQuestions = createDummyPreviewQuestions(request);
        }

        List<QuizQuestionResponse> questionResponses = previewQuestions.stream()
                .map(questionRequest -> toQuestionResponse(questionRequest, null))
                .toList();

        response.setQuestions(questionResponses);

        return response;
    }

    /**
     * 학생 배포
     *
     * 기존 POST /api/v1/quizzes/generate 에서 사용한다.
     * 프론트 미리보기에서 수정된 questions를 받아 DB에 저장한다.
     * 현재 구조에서는 DB에 저장되는 순간 학생에게 배포된 퀴즈로 본다.
     */
    @Transactional
    public QuizDetailResponse generateQuiz(QuizGenerateRequest request) {
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("배포할 문제가 없습니다. 먼저 퀴즈 미리보기를 생성해주세요.");
        }

        Quiz quiz = new Quiz();
        quiz.setTeacherId(request.getTeacherId());
        quiz.setMaterialId(request.getMaterialId());
        quiz.setTitle(request.getTitle());
        quiz.setDifficulty(request.getDifficulty());
        quiz.setStartPage(request.getStartPage());
        quiz.setEndPage(request.getEndPage());
        quiz.setAvailableFrom(request.getAvailableFrom());
        quiz.setAvailableUntil(request.getAvailableUntil());

        quizRepository.insertQuiz(quiz);

        int order = 1;

        for (QuizQuestionRequest questionRequest : request.getQuestions()) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizId(quiz.getQuizId());
            question.setQuestionType(
                    questionRequest.getQuestionType() == null
                            ? "MULTIPLE_CHOICE"
                            : questionRequest.getQuestionType()
            );
            question.setQuestionText(questionRequest.getQuestionText());
            question.setOptions(questionRequest.getOptions());
            question.setAnswerText(questionRequest.getAnswerText());
            question.setExplanation(questionRequest.getExplanation());

            if (questionRequest.getQuestionOrder() != null) {
                question.setQuestionOrder(questionRequest.getQuestionOrder());
            } else {
                question.setQuestionOrder(order);
            }

            quizRepository.insertQuizQuestion(question);
            order++;
        }

        return getQuiz(quiz.getQuizId());
    }

    /**
     * DB에 저장된 퀴즈 목록 조회
     *
     * 현재 구조에서는 DB에 저장된 퀴즈 = 학생에게 배포된 퀴즈로 본다.
     */
    public List<QuizListResponse> getQuizList() {
        return quizRepository.findQuizList();
    }

    /**
     * 퀴즈 상세 조회
     */
    public QuizDetailResponse getQuiz(Long quizId) {
        Quiz quiz = quizRepository.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        List<QuizQuestion> questions = quizRepository.findQuestionsByQuizId(quizId);

        return toQuizDetailResponse(quiz, questions);
    }

    /**
     * 퀴즈 정보 수정
     */
    @Transactional
    public QuizDetailResponse updateQuiz(Long quizId, QuizUpdateRequest request) {
        int updatedCount = quizRepository.updateQuiz(quizId, request);

        if (updatedCount == 0) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        return getQuiz(quizId);
    }

    /**
     * 퀴즈 삭제
     */
    @Transactional
    public void deleteQuiz(Long quizId) {
        int deletedCount = quizRepository.deleteQuiz(quizId);

        if (deletedCount == 0) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }
    }

    /**
     * 문제 수정
     */
    @Transactional
    public QuizDetailResponse updateQuestion(Long quizId, Long questionId, QuestionUpdateRequest request) {
        int updatedCount = quizRepository.updateQuestion(quizId, questionId, request);

        if (updatedCount == 0) {
            throw new IllegalArgumentException(
                    "존재하지 않는 문제입니다. quizId=" + quizId + ", questionId=" + questionId
            );
        }

        return getQuiz(quizId);
    }

    /**
     * 학생 퀴즈 응시 시작
     */
    @Transactional
    public QuizAttemptResponse startAttempt(Long quizId, QuizAttemptStartRequest request) {
        Quiz quiz = quizRepository.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        validateQuizAvailable(quiz);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizId(quizId);
        attempt.setStudentId(request.getStudentId());

        quizRepository.insertQuizAttempt(attempt);

        QuizAttempt savedAttempt = quizRepository.findAttemptById(quizId, attempt.getQuizAttemptId());

        return toAttemptResponse(savedAttempt);
    }

    /**
     * 학생 퀴즈 제출
     */
    @Transactional
    public QuizAttemptResultResponse submitQuiz(Long quizId, Long attemptId, QuizSubmitRequest request) {
        Quiz quiz = quizRepository.findQuizById(quizId);

        if (quiz == null) {
            throw new IllegalArgumentException("존재하지 않는 퀴즈입니다. quizId=" + quizId);
        }

        validateSubmitAvailable(quiz);

        QuizAttempt attempt = quizRepository.findAttemptById(quizId, attemptId);

        if (attempt == null) {
            throw new IllegalArgumentException("존재하지 않는 응시 기록입니다. attemptId=" + attemptId);
        }

        if (attempt.getSubmittedAt() != null) {
            throw new IllegalArgumentException("이미 제출된 퀴즈입니다. attemptId=" + attemptId);
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("제출된 답안이 없습니다.");
        }

        List<QuizQuestion> questions = quizRepository.findQuestionsByQuizId(quizId);

        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getQuizQuestionId, Function.identity()));

        int totalCount = request.getAnswers().size();
        int correctCount = 0;

        for (QuizAnswerSubmitRequest submittedAnswer : request.getAnswers()) {
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

            quizRepository.insertQuizAnswer(answer);
        }

        int score = totalCount == 0 ? 0 : (int) Math.round((correctCount * 100.0) / totalCount);

        quizRepository.updateAttemptResult(attemptId, score, totalCount, correctCount);

        return getAttemptResult(quizId, attemptId);
    }

    /**
     * 학생 퀴즈 결과 조회
     */
    public QuizAttemptResultResponse getAttemptResult(Long quizId, Long attemptId) {
        QuizAttempt attempt = quizRepository.findAttemptById(quizId, attemptId);

        if (attempt == null) {
            throw new IllegalArgumentException("존재하지 않는 응시 기록입니다. attemptId=" + attemptId);
        }

        List<QuizAnswerResultResponse> answers = quizRepository.findAnswerResultsByAttemptId(attemptId);

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

    private List<QuizQuestionRequest> createDummyPreviewQuestions(QuizGenerateRequest request) {
        String title = request.getTitle() == null ? "" : request.getTitle();

        if (title.contains("수학")) {
            return List.of(
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "2 + 3은 얼마인가요?",
                            """
                            [
                              {"number": 1, "text": "4"},
                              {"number": 2, "text": "5"},
                              {"number": 3, "text": "6"},
                              {"number": 4, "text": "7"}
                            ]
                            """,
                            "2",
                            "2에 3을 더하면 5입니다.",
                            1
                    ),
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "10에서 4를 빼면 얼마인가요?",
                            """
                            [
                              {"number": 1, "text": "5"},
                              {"number": 2, "text": "6"},
                              {"number": 3, "text": "7"},
                              {"number": 4, "text": "8"}
                            ]
                            """,
                            "2",
                            "10에서 4를 빼면 6입니다.",
                            2
                    ),
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "다음 중 가장 큰 수는 무엇인가요?",
                            """
                            [
                              {"number": 1, "text": "3"},
                              {"number": 2, "text": "8"},
                              {"number": 3, "text": "5"},
                              {"number": 4, "text": "1"}
                            ]
                            """,
                            "2",
                            "3, 8, 5, 1 중 가장 큰 수는 8입니다.",
                            3
                    )
            );
        }

        if (title.contains("과학")) {
            return List.of(
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "식물이 자라는 데 필요한 것은 무엇인가요?",
                            """
                            [
                              {"number": 1, "text": "물"},
                              {"number": 2, "text": "돌"},
                              {"number": 3, "text": "플라스틱"},
                              {"number": 4, "text": "유리"}
                            ]
                            """,
                            "1",
                            "식물이 자라려면 물, 햇빛, 공기 등이 필요합니다.",
                            1
                    ),
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "낮에 하늘에서 밝게 빛나는 것은 무엇인가요?",
                            """
                            [
                              {"number": 1, "text": "달"},
                              {"number": 2, "text": "태양"},
                              {"number": 3, "text": "구름"},
                              {"number": 4, "text": "비"}
                            ]
                            """,
                            "2",
                            "태양은 낮에 하늘을 밝게 비추는 별입니다.",
                            2
                    ),
                    createQuestionRequest(
                            "MULTIPLE_CHOICE",
                            "물이 얼면 무엇이 되나요?",
                            """
                            [
                              {"number": 1, "text": "얼음"},
                              {"number": 2, "text": "모래"},
                              {"number": 3, "text": "바람"},
                              {"number": 4, "text": "흙"}
                            ]
                            """,
                            "1",
                            "물이 차가워져 얼면 얼음이 됩니다.",
                            3
                    )
            );
        }

        return List.of(
                createQuestionRequest(
                        "MULTIPLE_CHOICE",
                        "다음 중 인사말로 알맞은 것은?",
                        """
                        [
                          {"number": 1, "text": "안녕하세요"},
                          {"number": 2, "text": "연필"},
                          {"number": 3, "text": "바나나"},
                          {"number": 4, "text": "자동차"}
                        ]
                        """,
                        "1",
                        "안녕하세요는 사람을 만났을 때 쓰는 인사말입니다.",
                        1
                ),
                createQuestionRequest(
                        "MULTIPLE_CHOICE",
                        "다음 중 문장의 끝에 쓰는 표시는 무엇인가요?",
                        """
                        [
                          {"number": 1, "text": "마침표"},
                          {"number": 2, "text": "ㄱ"},
                          {"number": 3, "text": "사과"},
                          {"number": 4, "text": "하늘"}
                        ]
                        """,
                        "1",
                        "문장이 끝날 때에는 마침표를 사용할 수 있습니다.",
                        2
                ),
                createQuestionRequest(
                        "MULTIPLE_CHOICE",
                        "다음 중 동물 이름은 무엇인가요?",
                        """
                        [
                          {"number": 1, "text": "책상"},
                          {"number": 2, "text": "강아지"},
                          {"number": 3, "text": "연필"},
                          {"number": 4, "text": "가방"}
                        ]
                        """,
                        "2",
                        "강아지는 동물입니다.",
                        3
                )
        );
    }

    private QuizQuestionRequest createQuestionRequest(
            String questionType,
            String questionText,
            String options,
            String answerText,
            String explanation,
            Integer questionOrder
    ) {
        QuizQuestionRequest request = new QuizQuestionRequest();
        request.setQuestionType(questionType);
        request.setQuestionText(questionText);
        request.setOptions(options);
        request.setAnswerText(answerText);
        request.setExplanation(explanation);
        request.setQuestionOrder(questionOrder);
        return request;
    }

    private QuizDetailResponse toQuizDetailResponse(Quiz quiz, List<QuizQuestion> questions) {
        QuizDetailResponse response = new QuizDetailResponse();

        response.setQuizId(quiz.getQuizId());
        response.setTeacherId(quiz.getTeacherId());
        response.setMaterialId(quiz.getMaterialId());
        response.setTitle(quiz.getTitle());
        response.setDifficulty(quiz.getDifficulty());
        response.setStartPage(quiz.getStartPage());
        response.setEndPage(quiz.getEndPage());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setCreatedAt(quiz.getCreatedAt());

        List<QuizQuestionResponse> questionResponses = questions.stream()
                .map(this::toQuestionResponse)
                .toList();

        response.setQuestions(questionResponses);

        return response;
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

    private QuizQuestionResponse toQuestionResponse(QuizQuestionRequest questionRequest, Long quizId) {
        QuizQuestionResponse response = new QuizQuestionResponse();

        response.setQuizQuestionId(null);
        response.setQuizId(quizId);
        response.setQuestionType(questionRequest.getQuestionType());
        response.setQuestionText(questionRequest.getQuestionText());
        response.setOptions(questionRequest.getOptions());
        response.setAnswerText(questionRequest.getAnswerText());
        response.setExplanation(questionRequest.getExplanation());
        response.setQuestionOrder(questionRequest.getQuestionOrder());

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
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }
}
