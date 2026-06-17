package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.repository.QuizRepository;
import com.example.myapp.quiz.repository.QuizRepository.QuizAttemptStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tool Calling: 교사가 등록한 퀴즈의 현황·통계를 조회한다.
 *
 * 예) "과학 퀴즈 몇 명이 풀었어?", "수학 퀴즈 현황 알려줘"
 */
@Slf4j
public class QuizQueryTool {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("M월 d일 HH:mm");

    private final Long teacherId;
    private final QuizRepository quizRepository;

    public QuizQueryTool(Long teacherId, QuizRepository quizRepository) {
        this.teacherId = teacherId;
        this.quizRepository = quizRepository;
    }

    /**
     * 퀴즈 현황을 조회한다.
     *
     * @param quizKeyword 퀴즈 제목 키워드 (예: "과학", "수학 3단원"). 전체 조회면 빈 문자열.
     */
    @Tool(description = "교사가 등록한 퀴즈의 현황과 응시 통계를 조회합니다. 몇 명이 풀었는지, 평균 점수 등을 알 수 있습니다.")
    public String queryQuizStats(
            @ToolParam(description = "퀴즈 제목 키워드 (예: '과학', '수학'). 전체 조회면 빈 문자열") String quizKeyword) {

        log.info("[QuizQueryTool] teacherId={} keyword={}", teacherId, quizKeyword);

        // 키워드가 없으면 빈 문자열로 전체 조회 (LIKE '%%' → 전체 매칭)
        String keyword = (quizKeyword == null || quizKeyword.isBlank()) ? "" : quizKeyword;
        List<Quiz> quizzes = quizRepository.findByTeacherAndKeyword(teacherId, keyword);

        if (quizzes.isEmpty()) {
            String msg = quizKeyword == null || quizKeyword.isBlank()
                    ? "등록된 퀴즈가 없습니다."
                    : "'" + quizKeyword + "' 키워드에 해당하는 퀴즈를 찾을 수 없습니다.";
            return msg;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("퀴즈 현황 (총 ").append(quizzes.size()).append("개)\n\n");

        for (int i = 0; i < quizzes.size(); i++) {
            Quiz q = quizzes.get(i);
            sb.append(i + 1).append(". ").append(q.getTitle());
            if (q.getDifficulty() != null) sb.append(" [").append(q.getDifficulty()).append("]");
            if (q.getCreatedAt() != null) sb.append(" — ").append(q.getCreatedAt().format(DISPLAY)).append(" 등록");
            sb.append("\n");

            try {
                QuizAttemptStats stats = quizRepository.getAttemptStats(q.getQuizId());
                sb.append("   응시 학생: ").append(stats.studentCount()).append("명");
                sb.append(" / 제출 완료: ").append(stats.submittedCount()).append("명\n");
                if (stats.submittedCount() > 0) {
                    sb.append("   평균 점수: ").append(String.format("%.1f", stats.avgScore())).append("점");
                    sb.append(" / 최고: ").append(stats.maxScore()).append("점");
                    sb.append(" / 최저: ").append(stats.minScore()).append("점\n");
                }
            } catch (Exception e) {
                log.warn("[QuizQueryTool] 통계 조회 실패 quizId={}", q.getQuizId(), e);
                sb.append("   (통계 조회 실패)\n");
            }

            if (i < quizzes.size() - 1) sb.append("\n");
        }

        return sb.toString();
    }
}
