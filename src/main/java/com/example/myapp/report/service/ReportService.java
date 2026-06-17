package com.example.myapp.report.service;

import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import com.example.myapp.counseling.entity.CounselingNote;
import com.example.myapp.counseling.mapper.CounselingNoteMapper;
import com.example.myapp.quiz.entity.QuizAttempt;
import com.example.myapp.quiz.mapper.QuizAttemptMapper;
import com.example.myapp.report.dto.QuizAnswerDetail;
import com.example.myapp.report.dto.ReportGenerateRequest;
import com.example.myapp.report.dto.ReportResponse;
import com.example.myapp.report.dto.ReportResult;
import com.example.myapp.report.entity.AchievementReport;
import com.example.myapp.report.mapper.AchievementReportMapper;
import com.example.myapp.report.mapper.ReportQuizDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AchievementReportMapper achievementReportMapper;
    private final QuizAttemptMapper quizAttemptMapper;
    private final ReportQuizDetailMapper reportQuizDetailMapper;
    private final CounselingNoteMapper counselingNoteMapper;
    private final ChatClient chatClient;

    @Value("classpath:prompts/report-generator.st")
    private Resource reportGeneratorPrompt;

    /**
     * 학생 지도 리포트 생성.
     * 퀴즈 응시 결과 + 문항별 응답 상세 + 상담 기록 → AI 분석 → achievement_report 저장.
     */
    @Transactional
    public ReportResponse generateReport(Long studentId, ReportGenerateRequest request) {
        List<QuizAttempt> attempts = quizAttemptMapper.findByStudentIdAndPeriod(
                studentId,
                request.periodStart(),
                request.periodEnd()
        );

        List<QuizAnswerDetail> answerDetails = reportQuizDetailMapper.findByStudentIdAndPeriod(
                studentId,
                request.periodStart(),
                request.periodEnd()
        );

        List<CounselingNote> notes = counselingNoteMapper.findByStudentIdAndPeriod(
                studentId,
                request.periodStart(),
                request.periodEnd()
        );

        if (attempts.isEmpty() && answerDetails.isEmpty() && notes.isEmpty()) {
            throw new BusinessException(ErrorCode.REPORT_DATA_INSUFFICIENT);
        }

        log.info(
                "[Report] 리포트 생성 시작 studentId={} attempts={} answers={} notes={}",
                studentId,
                attempts.size(),
                answerDetails.size(),
                notes.size()
        );

        String prompt = loadPrompt() + "\n\n" + buildInputData(attempts, answerDetails, notes);

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
     * AI에게 전달할 학생 데이터 문자열을 구성한다.
     * 점수 요약뿐 아니라 문항별 응답 상세를 함께 전달한다.
     */
    private String buildInputData(
            List<QuizAttempt> attempts,
            List<QuizAnswerDetail> answerDetails,
            List<CounselingNote> notes
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 퀴즈 응시 결과 요약\n");

        if (attempts.isEmpty()) {
            sb.append("- 해당 기간 내 응시 기록 없음\n");
        } else {
            int totalScore = 0;
            int totalCorrect = 0;
            int totalQuestions = 0;

            for (QuizAttempt attempt : attempts) {
                int score = attempt.getScore() != null ? attempt.getScore() : 0;
                int correct = attempt.getCorrectCount() != null ? attempt.getCorrectCount() : 0;
                int total = attempt.getTotalCount() != null ? attempt.getTotalCount() : 0;

                totalScore += score;
                totalCorrect += correct;
                totalQuestions += total;

                sb.append("- %s: %d점, 정답 %d개/%d개\n"
                        .formatted(
                                nullToDash(attempt.getQuizTitle()),
                                score,
                                correct,
                                total
                        ));
            }

            double averageScore = attempts.isEmpty()
                    ? 0
                    : (double) totalScore / attempts.size();

            double correctRate = totalQuestions > 0
                    ? (double) totalCorrect / totalQuestions * 100
                    : averageScore;

            sb.append("- 평균 점수: %.1f점\n".formatted(averageScore));
            sb.append("- 전체 정답률: %.1f%%\n".formatted(correctRate));
        }

        sb.append("\n## 문항별 응답 상세\n");

        if (answerDetails.isEmpty()) {
            sb.append("- 문항별 응답 상세 데이터 없음\n");
        } else {
            Map<Long, List<QuizAnswerDetail>> groupedByAttempt = new LinkedHashMap<>();

            for (QuizAnswerDetail detail : answerDetails) {
                groupedByAttempt
                        .computeIfAbsent(detail.quizAttemptId(), key -> new ArrayList<>())
                        .add(detail);
            }

            for (Map.Entry<Long, List<QuizAnswerDetail>> entry : groupedByAttempt.entrySet()) {
                List<QuizAnswerDetail> details = entry.getValue();

                if (details.isEmpty()) {
                    continue;
                }

                QuizAnswerDetail first = details.get(0);

                sb.append("\n### %s\n".formatted(nullToDash(first.quizTitle())));
                sb.append("- 점수: %d점\n".formatted(first.score() != null ? first.score() : 0));
                sb.append("- 정답: %d개/%d개\n".formatted(
                        first.correctCount() != null ? first.correctCount() : 0,
                        first.totalCount() != null ? first.totalCount() : 0
                ));

                for (QuizAnswerDetail detail : details) {
                    boolean correct = Boolean.TRUE.equals(detail.isCorrect());

                    sb.append("\n문항 %d. %s\n".formatted(
                            detail.questionOrder() != null ? detail.questionOrder() : 0,
                            nullToDash(detail.questionText())
                    ));

                    if (detail.options() != null && !detail.options().isBlank()) {
                        sb.append("- 선택지: %s\n".formatted(detail.options()));
                    }

                    sb.append("- 학생 답: %s\n".formatted(nullToDash(detail.studentAnswer())));
                    sb.append("- 정답: %s\n".formatted(nullToDash(detail.correctAnswer())));
                    sb.append("- 결과: %s\n".formatted(correct ? "정답" : "오답"));

                    if (detail.explanation() != null && !detail.explanation().isBlank()) {
                        sb.append("- 해설: %s\n".formatted(detail.explanation()));
                    }
                }
            }
        }

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

        sb.append("""
                
                ## 리포트 작성 지침
                - 평균 점수만 근거로 작성하지 말 것.
                - 문항별 오답 내용을 근거로 학생이 헷갈린 개념을 구체적으로 분석할 것.
                - 학생 답과 정답을 비교하여 어떤 사고 과정에서 오류가 있었는지 추론할 것.
                - 반복적으로 틀린 개념이나 문제 유형이 있다면 보완 필요 영역에 반영할 것.
                - 상담 기록이 있으면 퀴즈 결과와 함께 종합하여 학습 코멘트를 작성할 것.
                """);

        return sb.toString();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
