package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Tool Calling: 일정 조회.
 *
 * teacherId를 런타임에 주입받아 인스턴스 생성 → AgentExecutor에서 .tools(registerTool, queryTool) 로 등록.
 * LLM이 질문 의도에 따라 이 Tool 또는 ConsultationScheduleTool을 선택한다.
 */
@Slf4j
public class ScheduleQueryTool {

    private final Long teacherId;
    private final ScheduleMapper scheduleMapper;

    public ScheduleQueryTool(Long teacherId, ScheduleMapper scheduleMapper) {
        this.teacherId = teacherId;
        this.scheduleMapper = scheduleMapper;
    }

    /**
     * 특정 날짜 또는 기간의 일정을 조회한다.
     *
     * @param date      조회할 날짜 (ISO-8601, 예: 2026-06-17). 오늘이면 "TODAY", 내일이면 "TOMORROW" 도 허용.
     * @param endDate   기간 조회 시 종료 날짜 (ISO-8601). 단일 날짜 조회면 빈 문자열.
     * @param type      일정 유형 필터 (수업/상담/회의/수행평가/개인). 전체 조회면 빈 문자열.
     */
    @Tool(description = "교사의 일정을 날짜 또는 기간으로 조회합니다. 오늘/내일/이번 주 일정 조회에 사용하세요.")
    public String querySchedule(
            @ToolParam(description = "조회 시작 날짜 (ISO-8601, 예: 2026-06-17). 오늘이면 TODAY, 내일이면 TOMORROW") String date,
            @ToolParam(description = "기간 조회 시 종료 날짜 (ISO-8601). 단일 날짜면 빈 문자열") String endDate,
            @ToolParam(description = "일정 유형 필터: 수업, 상담, 회의, 수행평가, 개인 중 하나. 전체면 빈 문자열") String type) {

        LocalDate today = LocalDate.now();
        LocalDate from = resolveDate(date, today);
        if (from == null) {
            return "날짜 형식을 인식할 수 없습니다. 예) 2026-06-17";
        }

        LocalDate to = (endDate == null || endDate.isBlank()) ? from : resolveDate(endDate, today);
        if (to == null) to = from;

        String normalizedType = (type == null || type.isBlank()) ? null : type.trim();

        log.info("[ScheduleQueryTool] teacherId={} from={} to={} type={}", teacherId, from, to, normalizedType);

        List<Schedule> schedules;
        if (from.equals(to)) {
            schedules = normalizedType != null
                    ? scheduleMapper.findByTeacherAndDateAndType(teacherId, from, normalizedType)
                    : scheduleMapper.findByTeacherAndDate(teacherId, from);
        } else {
            schedules = scheduleMapper.findByTeacherAndDateRange(teacherId, from, to);
            if (normalizedType != null) {
                String ft = normalizedType;
                schedules = schedules.stream().filter(s -> ft.equals(s.getScheduleType())).toList();
            }
        }

        if (schedules.isEmpty()) {
            String rangeLabel = from.equals(to)
                    ? from.format(DateTimeFormatter.ofPattern("M월 d일"))
                    : from.format(DateTimeFormatter.ofPattern("M월 d일")) + " ~ " + to.format(DateTimeFormatter.ofPattern("M월 d일"));
            String typeLabel = normalizedType != null ? normalizedType + " " : "";
            return rangeLabel + "에 등록된 " + typeLabel + "일정이 없습니다.";
        }

        return formatSchedules(schedules, from, to);
    }

    private LocalDate resolveDate(String raw, LocalDate today) {
        if (raw == null || raw.isBlank()) return today;
        String trimmed = raw.trim().toUpperCase();
        if (trimmed.equals("TODAY")) return today;
        if (trimmed.equals("TOMORROW")) return today.plusDays(1);
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("[ScheduleQueryTool] 날짜 파싱 실패: {}", raw);
            return null;
        }
    }

    private String formatSchedules(List<Schedule> schedules, LocalDate from, LocalDate to) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("M월 d일");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        StringBuilder sb = new StringBuilder();
        String rangeLabel = from.equals(to)
                ? from.format(dateFmt)
                : from.format(dateFmt) + " ~ " + to.format(dateFmt);
        sb.append(rangeLabel).append(" 일정 (총 ").append(schedules.size()).append("건)\n\n");

        for (int i = 0; i < schedules.size(); i++) {
            Schedule s = schedules.get(i);
            sb.append(i + 1).append(". [").append(s.getScheduleType()).append("] ");
            sb.append(s.getTitle()).append("\n");
            sb.append("   시간: ").append(s.getScheduledAt().format(timeFmt));
            if (s.getEndAt() != null) {
                sb.append(" ~ ").append(s.getEndAt().format(timeFmt));
            }
            sb.append("\n");
            if (s.getLocation() != null) sb.append("   장소: ").append(s.getLocation()).append("\n");
            if (s.getStudentName() != null) sb.append("   학생: ").append(s.getStudentName()).append("\n");
            if (s.getParentName() != null) sb.append("   학부모: ").append(s.getParentName()).append("\n");
            if (s.getTopic() != null) sb.append("   주제: ").append(s.getTopic()).append("\n");
            if (i < schedules.size() - 1) sb.append("\n");
        }

        return sb.toString();
    }
}
