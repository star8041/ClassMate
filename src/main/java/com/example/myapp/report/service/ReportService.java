package com.example.myapp.report.service;

import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import com.example.myapp.counseling.dto.CounselingNoteResponse;
import com.example.myapp.counseling.entity.CounselingNote;
import com.example.myapp.counseling.mapper.CounselingNoteMapper;
import com.example.myapp.quiz.entity.QuizAttempt;
import com.example.myapp.quiz.mapper.QuizAttemptMapper;
import com.example.myapp.report.dto.ReportGenerateRequest;
import com.example.myapp.report.dto.ReportResponse;
import com.example.myapp.report.dto.ReportResult;
import com.example.myapp.report.entity.AchievementReport;
import com.example.myapp.report.mapper.AchievementReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AchievementReportMapper achievementReportMapper;
    private final QuizAttemptMapper quizAttemptMapper;
    private final CounselingNoteMapper counselingNoteMapper;
    private final ChatClient chatClient;

    @Value("classpath:prompts/report-generator.st")
    private Resource reportGeneratorPrompt;

    /**
     * 학생 지도 리포트 생성.
     * 퀴즈 응시 결과 + 상담 기록 → AI 분석 → achievement_report 저장.
     */
    @Transactional
    public ReportResponse generateReport(Long studentId, ReportGenerateRequest request) {
        List<QuizAttempt> attempts = quizAttemptMapper.findByStudentIdAndPeriod(
                studentId, request.periodStart(), request.periodEnd());

        List<CounselingNote> notes = counselingNoteMapper.findByStudentIdAndPeriod(
                studentId, request.periodStart(), request.periodEnd());

        if (attempts.isEmpty() && notes.isEmpty()) {
            throw new BusinessException(ErrorCode.REPORT_DATA_INSUFFICIENT);
        }

        log.info("[Report] 리포트 생성 시작 studentId={} attempts={} notes={}",
                studentId, attempts.size(), notes.size());

        String prompt = loadPrompt() + "\n\n" + buildInputData(attempts, notes);

        ReportResult result = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(ReportResult.class);

        AchievementReport report = AchievementReport.builder()
                .studentId(studentId)
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .summaryText(result.summaryText())
                .strengthText(result.strengthText())
                .weaknessText(result.weaknessText())
                .commentText(result.commentText())
                .build();

        Long id = achievementReportMapper.insert(report);
        log.info("[Report] 리포트 저장 완료 reportId={}", id);

        return ReportResponse.from(achievementReportMapper.findById(id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(Long studentId) {
        return achievementReportMapper.findByStudentId(studentId).stream()
                .map(ReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long reportId) {
        return ReportResponse.from(getOrThrow(reportId));
    }

    @Transactional
    public void deleteReport(Long reportId) {
        getOrThrow(reportId);
        achievementReportMapper.deleteById(reportId);
    }

    /**
     * 학생의 최근 상담 기록 1건을 반환한다 (리포트 페이지 상담 요약 섹션용).
     */
    @Transactional(readOnly = true)
    public List<CounselingNoteResponse> getRecentCounseling(Long studentId) {
        return counselingNoteMapper.findRecentByStudentId(studentId, 1).stream()
                .map(CounselingNoteResponse::from)
                .toList();
    }

    // ─── private ──────────────────────────────────────────────────────────────

    private AchievementReport getOrThrow(Long reportId) {
        return achievementReportMapper.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
    }

    private String loadPrompt() {
        try {
            return new String(reportGeneratorPrompt.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("리포트 프롬프트 로드 실패", e);
        }
    }

    /**
     * AI 에 전달할 학생 데이터 문자열을 구성한다.
     * 퀴즈 응시 결과 요약 + 상담 기록 요약.
     */
    private String buildInputData(List<QuizAttempt> attempts, List<CounselingNote> notes) {
        StringBuilder sb = new StringBuilder();

        // 퀴즈 결과 섹션
        sb.append("## 퀴즈 응시 결과\n");
        if (attempts.isEmpty()) {
            sb.append("- 해당 기간 내 응시 기록 없음\n");
        } else {
            int totalScore = 0;
            int totalPossible = 0;
            for (QuizAttempt a : attempts) {
                int score   = a.getScore()        != null ? a.getScore()        : 0;
                int total   = a.getTotalCount()   != null ? a.getTotalCount()   : 0;
                int correct = a.getCorrectCount() != null ? a.getCorrectCount() : 0;
                totalScore    += score;
                totalPossible += total;
                sb.append("- %s: %d/%d점 (정답 %d개/%d개)\n"
                        .formatted(a.getQuizTitle(), score, total, correct, total));
            }
            double avg = totalPossible > 0 ? (double) totalScore / totalPossible * 100 : 0;
            sb.append("- 전체 평균 정답률: %.1f%%\n".formatted(avg));
        }

        // 상담 기록 섹션
        sb.append("\n## 상담 기록\n");
        if (notes.isEmpty()) {
            sb.append("- 해당 기간 내 상담 기록 없음\n");
        } else {
            for (int i = 0; i < notes.size(); i++) {
                CounselingNote note = notes.get(i);
                sb.append("- 상담 %d\n".formatted(i + 1));
                if (note.getSummaryText() != null && !note.getSummaryText().isBlank()) {
                    sb.append("  요약: %s\n".formatted(note.getSummaryText()));
                }
                if (note.getFollowUpText() != null && !note.getFollowUpText().isBlank()) {
                    sb.append("  후속 조치: %s\n".formatted(note.getFollowUpText()));
                }
            }
        }

        return sb.toString();
    }
}
