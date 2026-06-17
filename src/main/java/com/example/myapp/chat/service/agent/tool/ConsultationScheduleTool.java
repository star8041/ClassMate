package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import com.example.myapp.student.entity.Student;
import com.example.myapp.student.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * Tool Calling: 모든 유형의 일정(상담/수업/회의/수행평가/개인)을 자동 등록.
 *
 * teacherId를 런타임에 주입받아 인스턴스 생성 → AgentExecutor에서 .tools(tool) 로 등록.
 */
@Slf4j
public class ConsultationScheduleTool {

    private static final Set<String> VALID_TYPES =
            Set.of("상담", "수업", "회의", "수행평가", "개인");

    private final Long teacherId;
    private final ScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;

    public ConsultationScheduleTool(Long teacherId, ScheduleMapper scheduleMapper, StudentMapper studentMapper) {
        this.teacherId = teacherId;
        this.scheduleMapper = scheduleMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * 일정을 DB에 등록하고 결과 메시지를 반환한다.
     *
     * @param scheduleType 일정 유형: 수업, 상담, 회의, 수행평가, 개인
     * @param title        제목 (언급 없으면 빈 문자열, 자동 생성됨)
     * @param scheduledAt  시작 일시 (ISO-8601, 예: 2026-06-22T10:00:00)
     * @param endAt        종료 일시 (ISO-8601, 언급 없으면 빈 문자열)
     * @param location     장소 (언급 없으면 빈 문자열)
     * @param topic        주제·내용 (언급 없으면 빈 문자열)
     * @param memo         메모 (언급 없으면 빈 문자열)
     * @param studentName  학생 이름 (상담 유형 시 사용, 아니면 빈 문자열)
     * @param parentName   학부모 이름 (상담 유형·학부모 상담 시 사용, 아니면 빈 문자열)
     */
    @Tool(description = "교사의 일정(상담/수업/회의/수행평가/개인)을 DB에 등록합니다.")
    public String registerSchedule(
            @ToolParam(description = "일정 유형: 수업, 상담, 회의, 수행평가, 개인 중 하나") String scheduleType,
            @ToolParam(description = "일정 제목 (언급 없으면 빈 문자열)") String title,
            @ToolParam(description = "시작 일시 (ISO-8601, 예: 2026-06-22T10:00:00)") String scheduledAt,
            @ToolParam(description = "종료 일시 (ISO-8601, 언급 없으면 빈 문자열)") String endAt,
            @ToolParam(description = "장소 (언급 없으면 빈 문자열)") String location,
            @ToolParam(description = "주제 또는 내용 (언급 없으면 빈 문자열)") String topic,
            @ToolParam(description = "메모 (언급 없으면 빈 문자열)") String memo,
            @ToolParam(description = "상담 대상 학생 이름 (상담 유형이 아니면 빈 문자열)") String studentName,
            @ToolParam(description = "학부모 이름 (상담 유형이 아니거나 학생 본인 상담이면 빈 문자열)") String parentName) {

        // scheduleType 정규화
        String type = (scheduleType == null || scheduleType.isBlank()) ? "개인" : scheduleType.trim();
        if (!VALID_TYPES.contains(type)) {
            // 유사 단어 처리
            if (type.contains("상담")) type = "상담";
            else if (type.contains("수업") || type.contains("강의")) type = "수업";
            else if (type.contains("회의") || type.contains("미팅")) type = "회의";
            else if (type.contains("수행") || type.contains("평가")) type = "수행평가";
            else type = "개인";
        }

        log.info("[ScheduleTool] type={} title={} at={} endAt={} location={} student={} teacherId={}",
                type, title, scheduledAt, endAt, location, studentName, teacherId);

        // 시작 일시 파싱
        LocalDateTime startDt;
        try {
            startDt = LocalDateTime.parse(scheduledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.error("[ScheduleTool] 시작 일시 파싱 실패: {}", scheduledAt, e);
            return "일시 형식을 인식할 수 없습니다. 예) 2026-06-22T10:00:00";
        }

        // 종료 일시 파싱 (선택)
        LocalDateTime endDt = null;
        if (!isBlank(endAt)) {
            try {
                endDt = LocalDateTime.parse(endAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                log.warn("[ScheduleTool] 종료 일시 파싱 실패, 무시: {}", endAt);
            }
        }

        // 제목 자동 생성
        String finalTitle = buildTitle(type, title, studentName, parentName);

        boolean hasParent = !isBlank(parentName);
        boolean hasStudent = !isBlank(studentName);

        // 상담 유형이고 학생 이름이 있으면 student_id 조회
        Long resolvedStudentId = null;
        if ("상담".equals(type) && hasStudent) {
            List<Student> matched = studentMapper.findByNameAndTeacherId(studentName, teacherId);
            if (matched.isEmpty()) {
                return "'" + studentName + "' 학생을 찾을 수 없습니다. 학생 이름을 다시 확인해 주세요.";
            }
            if (matched.size() > 1) {
                StringBuilder sb = new StringBuilder("'" + studentName + "' 이름의 학생이 여러 명입니다. 학번을 포함해 다시 알려주세요:\n");
                matched.forEach(s -> sb.append("- ").append(s.getStudentName()).append(" (학번: ").append(s.getStudentNumber()).append(")\n"));
                return sb.toString();
            }
            resolvedStudentId = matched.get(0).getStudentId();
        }

        Schedule schedule = Schedule.builder()
                .teacherId(teacherId)
                .studentId(resolvedStudentId)
                .scheduleType(type)
                .title(finalTitle)
                .topic(blankToNull(topic))
                .scheduledAt(startDt)
                .endAt(endDt)
                .location(blankToNull(location))
                .memo(blankToNull(memo))
                .studentName(hasStudent ? studentName : null)
                .parentName(hasParent ? parentName : null)
                .status("SCHEDULED")
                .build();

        Long scheduleId = scheduleMapper.insert(schedule);
        log.info("[ScheduleTool] 일정 등록 완료 scheduleId={} type={}", scheduleId, type);

        DateTimeFormatter display = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
        StringBuilder result = new StringBuilder("✅ ");
        result.append(type).append(" 일정이 등록되었습니다.");
        result.append("\n제목: ").append(finalTitle);
        result.append("\n일시: ").append(startDt.format(display));
        if (endDt != null) result.append(" ~ ").append(endDt.format(DateTimeFormatter.ofPattern("HH:mm")));
        if (hasStudent) result.append("\n학생: ").append(studentName);
        if (hasParent) result.append("\n학부모: ").append(parentName);
        if (!isBlank(location)) result.append("\n장소: ").append(location);
        if (!isBlank(topic)) result.append("\n주제: ").append(topic);
        if (!isBlank(memo)) result.append("\n메모: ").append(memo);

        return result.toString();
    }

    /** 제목이 없으면 유형에 맞게 자동 생성 */
    private String buildTitle(String type, String title, String studentName, String parentName) {
        if (!isBlank(title)) return title.trim();

        return switch (type) {
            case "상담" -> {
                boolean hasParent = !isBlank(parentName);
                boolean hasStudent = !isBlank(studentName);
                if (hasStudent && hasParent) yield studentName + " 학부모 상담";
                else if (hasStudent) yield studentName + " 학생 상담";
                else yield "상담";
            }
            case "수업" -> !isBlank(studentName) ? studentName + " 수업" : "수업";
            case "회의" -> "교직원 회의";
            case "수행평가" -> "수행평가";
            default -> "개인 일정";
        };
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
