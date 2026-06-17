package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.domain.QuizQuestion;
import com.example.myapp.quiz.repository.QuizRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;

/**
 * Tool Calling: LLM이 생성한 퀴즈 문제를 DB에 저장한다.
 *
 * 퀴즈 생성 흐름에서 두 번째로 호출됨:
 *   1. QuizPageFetchTool → 페이지 텍스트
 *   2. LLM이 문제 JSON 생성
 *   3. QuizSaveTool → DB 저장 (이 Tool)
 *
 * questionsJson 형식:
 * [
 *   {
 *     "questionType": "MULTIPLE_CHOICE",
 *     "questionText": "문제 내용",
 *     "options": "[{\"number\":1,\"text\":\"①선택지\"},{\"number\":2,\"text\":\"②선택지\"}]",
 *     "answerText": "1",
 *     "explanation": "해설"
 *   }
 * ]
 */
@Slf4j
public class QuizSaveTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Long teacherId;
    private final QuizRepository quizRepository;

    public QuizSaveTool(Long teacherId, QuizRepository quizRepository) {
        this.teacherId = teacherId;
        this.quizRepository = quizRepository;
    }

    /**
     * 생성된 퀴즈를 DB에 저장한다.
     *
     * @param title        퀴즈 제목
     * @param difficulty   난이도: 쉬움, 보통, 어려움
     * @param materialId   교재 ID (QuizPageFetchTool 결과에서 추출, 없으면 0)
     * @param startPage    시작 페이지 (없으면 0)
     * @param endPage      종료 페이지 (없으면 0)
     * @param questionsJson JSON 배열 문자열 — questionType, questionText, options, answerText, explanation
     */
    @Tool(description = "LLM이 생성한 퀴즈 문제 목록을 DB에 저장합니다. QuizPageFetchTool로 교재 내용을 가져온 뒤 문제를 생성하고 이 Tool을 호출하세요.")
    public String saveQuiz(
            @ToolParam(description = "퀴즈 제목 (예: '과학교과서 10-15쪽 퀴즈')") String title,
            @ToolParam(description = "난이도: 쉬움, 보통, 어려움 중 하나") String difficulty,
            @ToolParam(description = "교재 ID (QuizPageFetchTool 결과의 materialId 값, 없으면 0)") long materialId,
            @ToolParam(description = "퀴즈 출제 시작 페이지 (없으면 0)") int startPage,
            @ToolParam(description = "퀴즈 출제 종료 페이지 (없으면 0)") int endPage,
            @ToolParam(description = """
                    문제 JSON 배열. 각 객체:
                    {
                      "questionType": "MULTIPLE_CHOICE | SHORT_ANSWER | ESSAY",
                      "questionText": "문제 내용",
                      "options": "[{\\"number\\":1,\\"text\\":\\"선택지1\\"},...]" (객관식만, 주관식은 null),
                      "answerText": "정답",
                      "explanation": "해설"
                    }
                    """) String questionsJson) {

        log.info("[QuizSaveTool] title={} difficulty={} materialId={} pages={}-{}", title, difficulty, materialId, startPage, endPage);

        // 퀴즈 저장
        Quiz quiz = new Quiz();
        quiz.setTeacherId(teacherId);
        quiz.setMaterialId(materialId > 0 ? materialId : null);
        quiz.setTitle(title);
        quiz.setDifficulty(difficulty);
        quiz.setStartPage(startPage > 0 ? startPage : null);
        quiz.setEndPage(endPage > 0 ? endPage : null);
        quizRepository.insertQuiz(quiz);

        Long quizId = quiz.getQuizId();
        log.info("[QuizSaveTool] 퀴즈 저장 완료 quizId={}", quizId);

        // 문제 파싱 및 저장
        int savedCount = 0;
        try {
            List<Map<String, Object>> questions = MAPPER.readValue(questionsJson, new TypeReference<>() {});
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                QuizQuestion question = new QuizQuestion();
                question.setQuizId(quizId);
                question.setQuestionType(str(q, "questionType", "MULTIPLE_CHOICE"));
                question.setQuestionText(str(q, "questionText", ""));
                question.setOptions(str(q, "options", null));
                question.setAnswerText(str(q, "answerText", ""));
                question.setExplanation(str(q, "explanation", null));
                question.setQuestionOrder(i + 1);
                quizRepository.insertQuizQuestion(question);
                savedCount++;
            }
        } catch (Exception e) {
            log.error("[QuizSaveTool] 문제 JSON 파싱 실패: {}", questionsJson, e);
            return "퀴즈 제목은 저장됐지만 문제 파싱에 실패했습니다. (quizId=" + quizId + ")";
        }

        return "✅ 퀴즈가 등록되었습니다!\n제목: " + title + "\n난이도: " + difficulty +
                "\n문제 수: " + savedCount + "문제\n퀴즈 ID: " + quizId +
                "\n\n학생들이 퀴즈 페이지에서 응시할 수 있습니다.";
    }

    private static String str(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        String s = val.toString().trim();
        return s.isEmpty() ? defaultVal : s;
    }
}
