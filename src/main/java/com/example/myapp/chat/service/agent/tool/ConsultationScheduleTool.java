package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.counseling.entity.Schedule;
import com.example.myapp.counseling.mapper.ScheduleMapper;
import com.example.myapp.student.entity.Student;
import com.example.myapp.student.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tool Calling: 상담 일정 자동 등록.
 *
 * teacherId를 런타임에 주입받아 인스턴스 생성 → AgentExecutor에서 .tools(tool) 로 등록.
 */
@Slf4j
public class ConsultationScheduleTool {

    private final Long teacherId;
    private final ScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;

    public ConsultationScheduleTool(Long teacherId,
                                    ScheduleMapper scheduleMapper,
                                    StudentMapper studentMapper) {
        this.teacherId = teacherId;
        this.scheduleMapper = scheduleMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * 상담 일정을 DB에 등록하고 결과 메시지를 반환한다.
     *
     * @param studentName   학생 이름 (예: "윤여옥")
     * @param scheduledAt   ISO-8601 형식의 상담 일시 (예: "2025-06-22T10:00:00")
     * @param partnerType   상담 상대 유형: "학부모" 또는 "학생"
     * @param topic         상담 주제 (선택, 없으면 빈 문자열)
     */
    @Tool(description = "교사와 학부모·학생 간 상담 일정을 DB에 등록합니다.")
    public String registerConsultationSchedule(
            @ToolParam(description = "상담할 학생 이름") String studentName,
            @ToolParam(description = "상담 일시 (ISO-8601, 예: 2025-06-22T10:00:00)") String scheduledAt,
            @ToolParam(description = "상담 상대 유형: 학부모 또는 학생") String partnerType,
            @ToolParam(description = "상담 주제 (없으면 빈 문자열)") String topic) {

        log.info("[ConsultationTool] studentName={} scheduledAt={} partner={} teacherId={}",
                studentName, scheduledAt, partnerType, teacherId);

        // 1. 학생 조회
        List<Student> students = studentMapper.findByNameAndTeacherId(studentName, teacherId);
        if (students.isEmpty()) {
            return "'" + studentName + "' 학생을 찾을 수 없습니다. 학생 이름을 확인해주세요.";
        }
        if (students.size() > 1) {
            return "'" + studentName + "' 이름의 학생이 여러 명입니다. 학번도 함께 알려주세요.";
        }
        Student student = students.get(0);

        // 2. 일시 파싱
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(scheduledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.error("[ConsultationTool] 일시 파싱 실패: {}", scheduledAt, e);
            return "상담 일시 형식을 인식할 수 없습니다. 예) 2025-06-22T10:00:00";
        }

        // 3. 제목 생성
        String title = studentName + " " + partnerType + " 상담";

        // 4. DB 저장
        Schedule schedule = Schedule.builder()
                .teacherId(teacherId)
                .studentId(student.getStudentId())
                .scheduleType("CONSULTATION")
                .title(title)
                .topic(topic)
                .scheduledAt(dateTime)
                .status("SCHEDULED")
                .build();

        Long scheduleId = scheduleMapper.insert(schedule);
        log.info("[ConsultationTool] 상담 일정 등록 완료 scheduleId={}", scheduleId);

        DateTimeFormatter display = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
        return String.format(
                "✅ 상담 일정이 등록되었습니다.\n학생: %s\n상대: %s\n일시: %s\n주제: %s",
                studentName, partnerType, dateTime.format(display),
                topic.isBlank() ? "미정" : topic);
    }
}
