package com.example.myapp.quiz.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.myapp.material.entity.Material;
import com.example.myapp.material.entity.MaterialPage;
import com.example.myapp.material.mapper.MaterialMapper;
import com.example.myapp.material.mapper.MaterialPageMapper;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final QuizRepository quizRepository;
    private final MaterialMapper materialMapper;
    private final MaterialPageMapper materialPageMapper;
    private final ChatClient chatClient;

    /**
     * 퀴즈 미리보기 생성
     *
     * DB에 저장하지 않는다.
     * 선택한 강의자료의 페이지 내용을 기반으로 LLM이 문제를 생성한다.
     */
    public QuizDetailResponse previewQuiz(QuizGenerateRequest request) {
        validatePreviewRequest(request);

        Material material = materialMapper.findById(request.getMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의자료입니다. materialId=" + request.getMaterialId()));

        if (request.getTeacherId() != null && !material.getTeacherId().equals(request.getTeacherId())) {
            throw new IllegalArgumentException("해당 교사의 강의자료가 아닙니다. materialId=" + request.getMaterialId());
        }

        List<MaterialPage> pages = materialPageMapper.findByPageRange(
                request.getMaterialId(),
                request.getStartPage(),
                request.getEndPage()
        );

        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException(
                    "선택한 페이지 범위의 교재 내용을 찾을 수 없습니다. pages="
                            + request.getStartPage() + "-" + request.getEndPage()
            );
        }

        String pageContent = pages.stream()
                .map(page -> "[%d페이지]\n%s".formatted(page.getPageNumber(), nullToEmpty(page.getPageText())))
                .collect(Collectors.joining("\n\n---\n\n"));

        if (pageContent.isBlank()) {
            throw new IllegalArgumentException("선택한 페이지에 추출된 텍스트가 없습니다.");
        }

        String title = normalizeTitle(request, material);

        List<QuizQuestionRequest> previewQuestions = generateQuestionsByAi(
                material,
                request,
                pageContent
        );

        QuizDetailResponse response = new QuizDetailResponse();

        response.setQuizId(null);
        response.setTeacherId(request.getTeacherId());
        response.setMaterialId(request.getMaterialId());
        response.setTitle(title);
        response.setDifficulty(request.getDifficulty());
        response.setStartPage(request.getStartPage());
        response.setEndPage(request.getEndPage());
        response.setAvailableFrom(request.getAvailableFrom());
        response.setAvailableUntil(request.getAvailableUntil());
        response.setCreatedAt(LocalDateTime.now());

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

        if (request.getTeacherId() == null) {
            throw new IllegalArgumentException("교사 정보가 없습니다.");
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
     * 학생용 퀴즈 목록 조회
     */
    public List<QuizListResponse> getStudentQuizList(Long studentId) {
        return quizRepository.findStudentQuizList(studentId);
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

        boolean alreadySubmitted = quizRepository.existsSubmittedAttempt(
                quizId,
                request.getStudentId()
        );

        if (alreadySubmitted) {
            throw new IllegalArgumentException("이미 제출한 퀴즈는 다시 응시할 수 없습니다. quizId=" + quizId);
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

        boolean alreadySubmitted = quizRepository.existsSubmittedAttempt(
                quizId,
                attempt.getStudentId()
        );

        if (alreadySubmitted) {
            throw new IllegalArgumentException("이미 제출한 퀴즈는 다시 제출할 수 없습니다. quizId=" + quizId);
        }

        if (request.getAnswers() == null) {
            throw new IllegalArgumentException("제출된 답안이 없습니다.");
        }

        List<QuizQuestion> questions = quizRepository.findQuestionsByQuizId(quizId);

        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("퀴즈에 포함된 문제가 없습니다. quizId=" + quizId);
        }

        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(
                        QuizQuestion::getQuizQuestionId,
                        Function.identity()
                ));

        Map<Long, QuizAnswerSubmitRequest> submittedAnswerMap = request.getAnswers().stream()
                .filter(answer -> answer.getQuizQuestionId() != null)
                .collect(Collectors.toMap(
                        QuizAnswerSubmitRequest::getQuizQuestionId,
                        Function.identity(),
                        (oldValue, newValue) -> newValue
                ));

        int totalCount = questions.size();
        int correctCount = 0;

        for (QuizQuestion question : questions) {
            QuizAnswerSubmitRequest submittedAnswer = submittedAnswerMap.get(question.getQuizQuestionId());

            String submittedAnswerText = submittedAnswer == null
                    ? ""
                    : submittedAnswer.getAnswerText();

            boolean isCorrect = normalize(question.getAnswerText())
                    .equals(normalize(submittedAnswerText));

            if (isCorrect) {
                correctCount++;
            }

            QuizAnswer answer = new QuizAnswer();
            answer.setQuizAttemptId(attemptId);
            answer.setQuizQuestionId(question.getQuizQuestionId());
            answer.setAnswerText(submittedAnswerText);
            answer.setIsCorrect(isCorrect);

            quizRepository.insertQuizAnswer(answer);
        }

        int score = totalCount == 0
                ? 0
                : (int) Math.round((correctCount * 100.0) / totalCount);

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

    private List<QuizQuestionRequest> generateQuestionsByAi(
            Material material,
            QuizGenerateRequest request,
            String pageContent
    ) {
        String prompt = buildQuizGenerationPrompt(material, request, pageContent);

        log.info(
                "[QuizService] AI 퀴즈 생성 시작 materialId={} pages={}-{} count={}",
                request.getMaterialId(),
                request.getStartPage(),
                request.getEndPage(),
                request.getQuestionCount()
        );

        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("[QuizService] AI 퀴즈 생성 원문 응답={}", aiResponse);

        List<QuizQuestionRequest> questions = parseAiQuestions(aiResponse);

        if (questions.isEmpty()) {
            throw new IllegalArgumentException("AI가 생성한 문제가 없습니다.");
        }

        int expectedCount = request.getQuestionCount() == null ? questions.size() : request.getQuestionCount();

        if (questions.size() > expectedCount) {
            questions = questions.subList(0, expectedCount);
        }

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestionRequest question = questions.get(i);

            if (question.getQuestionType() == null || question.getQuestionType().isBlank()) {
                question.setQuestionType("MULTIPLE_CHOICE");
            }

            question.setQuestionOrder(i + 1);

            if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
                throw new IllegalArgumentException("AI가 생성한 문제 내용이 비어 있습니다.");
            }

            if (question.getOptions() == null || question.getOptions().isBlank()) {
                throw new IllegalArgumentException("AI가 생성한 객관식 선택지가 비어 있습니다.");
            }

            if (question.getAnswerText() == null || question.getAnswerText().isBlank()) {
                throw new IllegalArgumentException("AI가 생성한 정답이 비어 있습니다.");
            }
        }

        return questions;
    }

    private String buildQuizGenerationPrompt(
            Material material,
            QuizGenerateRequest request,
            String pageContent
    ) {
        int questionCount = request.getQuestionCount() == null ? 5 : request.getQuestionCount();
        String difficulty = request.getDifficulty() == null || request.getDifficulty().isBlank()
                ? "중간"
                : request.getDifficulty();

        return """
                너는 초등학생용 퀴즈를 만드는 교사용 AI야.
                아래 교재 내용을 바탕으로 객관식 퀴즈를 생성해.

                [생성 조건]
                - 문제 수: %d개
                - 난이도: %s
                - 문제 유형: 전부 MULTIPLE_CHOICE
                - 각 문제는 선택지 4개를 가져야 함
                - answerText에는 정답 선택지 번호만 문자열로 넣기. 예: "1", "2", "3", "4"
                - explanation은 초등학생이 이해할 수 있게 짧고 명확하게 작성
                - 교재 내용에 없는 사실을 임의로 만들지 말 것
                - 반드시 아래 JSON 형식만 반환
                - 마크다운 코드블록, 설명 문장, 주석을 절대 붙이지 말 것

                [반환 형식]
                [
                  {
                    "questionType": "MULTIPLE_CHOICE",
                    "questionText": "문제 내용",
                    "options": [
                      {"number": 1, "text": "선택지 1"},
                      {"number": 2, "text": "선택지 2"},
                      {"number": 3, "text": "선택지 3"},
                      {"number": 4, "text": "선택지 4"}
                    ],
                    "answerText": "1",
                    "explanation": "해설"
                  }
                ]

                [교재 정보]
                파일명: %s
                과목: %s
                페이지 범위: %d~%d쪽

                [교재 내용]
                %s
                """.formatted(
                questionCount,
                difficulty,
                nullToEmpty(material.getFileName()),
                nullToEmpty(material.getSubject()),
                request.getStartPage(),
                request.getEndPage(),
                pageContent
        );
    }

    private List<QuizQuestionRequest> parseAiQuestions(String aiResponse) {
        try {
            String json = extractJson(aiResponse);

            if (json.startsWith("{")) {
                Map<String, Object> wrapper = OBJECT_MAPPER.readValue(
                        json,
                        new TypeReference<Map<String, Object>>() {}
                );

                Object questions = wrapper.get("questions");

                if (questions == null) {
                    questions = wrapper.get("data");
                }

                if (questions == null) {
                    throw new IllegalArgumentException("AI 응답에서 questions를 찾을 수 없습니다.");
                }

                json = OBJECT_MAPPER.writeValueAsString(questions);
            }

            List<Map<String, Object>> rawQuestions = OBJECT_MAPPER.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            return rawQuestions.stream()
                    .map(this::toQuizQuestionRequestFromMap)
                    .toList();

        } catch (Exception e) {
            log.error("[QuizService] AI 문제 JSON 파싱 실패. response={}", aiResponse, e);
            throw new IllegalArgumentException("AI가 생성한 문제 형식을 해석하지 못했습니다.");
        }
    }

    private QuizQuestionRequest toQuizQuestionRequestFromMap(Map<String, Object> raw) {
        QuizQuestionRequest request = new QuizQuestionRequest();

        request.setQuestionType(stringValue(raw.getOrDefault("questionType", "MULTIPLE_CHOICE")));
        request.setQuestionText(stringValue(raw.get("questionText")));
        request.setAnswerText(stringValue(raw.get("answerText")));
        request.setExplanation(stringValue(raw.get("explanation")));

        Object optionsValue = raw.get("options");

        try {
            if (optionsValue == null) {
                request.setOptions(null);
            } else if (optionsValue instanceof String optionsString) {
                request.setOptions(optionsString);
            } else {
                request.setOptions(OBJECT_MAPPER.writeValueAsString(optionsValue));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("선택지 변환에 실패했습니다.", e);
        }

        return request;
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다.");
        }

        String trimmed = text.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "")
                    .replaceFirst("^```", "")
                    .replaceFirst("```$", "")
                    .trim();
        }

        int arrayStart = trimmed.indexOf("[");
        int arrayEnd = trimmed.lastIndexOf("]");

        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }

        int objectStart = trimmed.indexOf("{");
        int objectEnd = trimmed.lastIndexOf("}");

        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }

        throw new IllegalArgumentException("AI 응답에서 JSON을 찾을 수 없습니다.");
    }

    private void validatePreviewRequest(QuizGenerateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("퀴즈 생성 요청이 비어 있습니다.");
        }

        if (request.getTeacherId() == null) {
            throw new IllegalArgumentException("교사 정보가 없습니다.");
        }

        if (request.getMaterialId() == null) {
            throw new IllegalArgumentException("강의자료를 선택해주세요.");
        }

        if (request.getStartPage() == null || request.getStartPage() < 1) {
            throw new IllegalArgumentException("시작 쪽은 1 이상이어야 합니다.");
        }

        if (request.getEndPage() == null || request.getEndPage() < 1) {
            throw new IllegalArgumentException("끝 쪽은 1 이상이어야 합니다.");
        }

        if (request.getStartPage() > request.getEndPage()) {
            throw new IllegalArgumentException("시작 쪽은 끝 쪽보다 클 수 없습니다.");
        }

        if (request.getQuestionCount() == null || request.getQuestionCount() < 1 || request.getQuestionCount() > 50) {
            throw new IllegalArgumentException("문제 수는 1개 이상 50개 이하로 입력해주세요.");
        }
    }

    private String normalizeTitle(QuizGenerateRequest request, Material material) {
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            return request.getTitle();
        }

        return "%s %d-%d쪽 퀴즈".formatted(
                material.getFileName(),
                request.getStartPage(),
                request.getEndPage()
        );
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }

        return String.valueOf(value).trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
