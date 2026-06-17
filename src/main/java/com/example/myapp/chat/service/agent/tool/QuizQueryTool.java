package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.quiz.domain.Quiz;
import com.example.myapp.quiz.repository.QuizRepository;
import com.example.myapp.quiz.repository.QuizRepository.QuizAttemptStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
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
     * @param quizKeyword 퀴즈 제목 키워드. 전체 조회면 빈 문자열.
     * @param startDate   조회 시작일 (yyyy-MM-dd). "TODAY"=오늘, "THIS_WEEK"=이번주 월요일, 없으면 null.
     * @param endDate     조회 종료일 (yyyy-MM-dd). "TODAY"=오늘, 없으면 null.
     */
    @Tool(description = "교사가 등록한 퀴즈의 현황과 응시 통계를 조회합니다. 기간(이번주, 오늘 등)과 키워드로 필터링 가능합니다.")
    public String queryQuizStats(
            @ToolParam(description = "퀴즈 제목 키워드 (예: '과학', '수학'). 전체 조회면 빈 문자열") String quizKeyword,
            @ToolParam(description = "조회 시작일 yyyy-MM-dd. 이번주=THIS_WEEK, 오늘=TODAY, 전체=빈 문자열") String startDate,
            @ToolParam(description = "조회 종료일 yyyy-MM-dd. 오늘=TODAY, 전체=빈 문자열") String endDate) {

        log.info("[QuizQueryTool] teacherId={} keyword={} startDate={} endDate={}", teacherId, quizKeyword, startDate, endDate);

        String keyword = (quizKeyword == null || quizKeyword.isBlank()) ? "" : quizKeyword;
        LocalDate from = resolveDate(startDate, true);
        LocalDate to   = resolveDate(endDate, false);

        List<Quiz> quizzes = quizRepository.findByTeacherAndKeywordAndDateRange(teacherId, keyword, from, to);

        if (quizzes.isEmpty()) {
            return (quizKeyword == null || quizKeyword.isBlank())
                    ? "해당 기간에 등록된 퀴즈가 없습니다."
                    : "'" + quizKeyword + "' 키워드에 해당하는 퀴즈를 찾을 수 없습니다.";
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

    /**
     * 날짜 문자열을 LocalDate로 변환한다.
     * - "TODAY"       → 오늘
     * - "THIS_WEEK"   → 이번주 월요일 (isStart=true) / 오늘 (isStart=false)
     * - yyyy-MM-dd    → 파싱
     * - 빈 문자열/null → null (필터 없음)
     */
    private LocalDate resolveDate(String value, boolean isStart) {
        if (value == null || value.isBlank()) return null;
        LocalDate today = LocalDate.now();
        return switch (value.toUpperCase()) {
            case "TODAY"     -> today;
            case "THIS_WEEK" -> isStart
                    ? today.with(java.time.DayOfWeek.MONDAY)
                    : today;
            default -> {
                try { yield LocalDate.parse(value); }
                catch (Exception e) { yield null; }
            }
        };
    }
}
