package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.schedule.entity.Schedule;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tool Calling: 상담 일정 자동 등록.
 *
 * teacherId를 런타임에 주입받아 인스턴스 생성 → AgentExecutor에서 .tools(tool) 로 등록.
 * schedule.mapper.ScheduleMapper (팀원 구현) 사용.
 */
@Slf4j
public class ConsultationScheduleTool {

    private final Long teacherId;
    private final ScheduleMapper scheduleMapper;

    public ConsultationScheduleTool(Long teacherId, ScheduleMapper scheduleMapper) {
        this.teacherId = teacherId;
        this.scheduleMapper = scheduleMapper;
    }

    /**
     * 상담 일정을 DB에 등록하고 결과 메시지를 반환한다.
     *
     * @param studentName  학생 이름 (예: "윤여옥")
     * @param parentName   학부모 이름 (없으면 빈 문자열)
     * @param scheduledAt  ISO-8601 형식의 상담 일시 (예: "2025-06-22T10:00:00")
     * @param topic        상담 주제 (없으면 빈 문자열)
     */
    @Tool(description = "교사와 학부모·학생 간 상담 일정을 DB에 등록합니다.")
    public String registerConsultationSchedule(
            @ToolParam(description = "상담 대상 학생 이름") String studentName,
            @ToolParam(description = "학부모 이름 (학생 본인 상담이면 빈 문자열)") String parentName,
            @ToolParam(description = "상담 일시 (ISO-8601, 예: 2025-06-22T10:00:00)") String scheduledAt,
            @ToolParam(description = "상담 주제 (없으면 빈 문자열)") String topic) {

        log.info("[ConsultationTool] student={} parent={} at={} teacherId={}",
                studentName, parentName, scheduledAt, teacherId);

        // 일시 파싱
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(scheduledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.error("[ConsultationTool] 일시 파싱 실패: {}", scheduledAt, e);
            return "상담 일시 형식을 인식할 수 없습니다. 예) 2025-06-22T10:00:00";
        }

        // 제목 생성
        boolean hasParent = parentName != null && !parentName.isBlank();
        String title = studentName + (hasParent ? " 학부모 상담" : " 학생 상담");

        // DB 저장
        Schedule schedule = Schedule.builder()
                .teacherId(teacherId)
                .scheduleType("상담")
                .title(title)
                .topic(topic)
                .scheduledAt(dateTime)
                .studentName(studentName)
                .parentName(hasParent ? parentName : null)
                .status("SCHEDULED")
                .build();

        Long scheduleId = scheduleMapper.insert(schedule);
        log.info("[ConsultationTool] 상담 일정 등록 완료 scheduleId={}", scheduleId);

        DateTimeFormatter display = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
        return String.format(
                "✅ 상담 일정이 등록되었습니다.\n학생: %s%s\n일시: %s\n주제: %s",
                studentName,
                hasParent ? "\n학부모: " + parentName : "",
                dateTime.format(display),
                (topic == null || topic.isBlank()) ? "미정" : topic);
    }
}
