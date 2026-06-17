package com.example.myapp.chat.service.agent;

import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.service.agent.tool.ConsultationScheduleTool;
import com.example.myapp.counseling.mapper.CounselingNoteMapper;
import com.example.myapp.chat.service.agent.tool.QuizPageFetchTool;
import com.example.myapp.chat.service.agent.tool.QuizQueryTool;
import com.example.myapp.chat.service.agent.tool.QuizSaveTool;
import com.example.myapp.chat.service.agent.tool.ScheduleQueryTool;
import com.example.myapp.material.mapper.MaterialMapper;
import com.example.myapp.material.mapper.MaterialPageMapper;
import com.example.myapp.quiz.repository.QuizRepository;
import com.example.myapp.schedule.mapper.ScheduleMapper;
import com.example.myapp.student.mapper.StudentMapper;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * AgentContext를 받아 역할별 시스템 프롬프트로 AI 응답을 스트리밍한다.
 *
 * TEACHER → teacher-system.st (전문적·간결, 교수법 조언)
 * STUDENT → student-system.st (친절·쉬운 설명, 격려)
 * SCHEDULE_CONSULTATION → Tool Calling으로 DB 저장 후 결과 스트리밍
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final ChatModel chatModel;
    private final ScheduleMapper scheduleMapper;
    private final StudentMapper studentMapper;
    private final MaterialMapper materialMapper;
    private final MaterialPageMapper materialPageMapper;
    private final QuizRepository quizRepository;
    private final CounselingNoteMapper counselingNoteMapper;

    @Value("classpath:prompts/teacher-system.st")
    private Resource teacherSystemPrompt;

    @Value("classpath:prompts/student-system.st")
    private Resource studentSystemPrompt;

    @Value("classpath:prompts/quiz-system.st")
    private Resource quizSystemPrompt;

    /** SSE 스트리밍 응답 */
    public Flux<String> executeStream(AgentContext context) {
        log.debug("[Executor] role={} intent={}", context.getRole(), context.getIntentResult().intentType());

        if (context.getIntentResult().intentType() == IntentType.SCHEDULE_CONSULTATION) {
            return executeScheduleConsultationStream(context);
        }
        if (context.getIntentResult().intentType() == IntentType.QUIZ_TOOL) {
            return executeQuizToolStream(context);
        }

        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(context.getRole()))
                .user(buildUserPrompt(context))
                .stream()
                .content();
    }

    /** 논-스트리밍 단건 호출 (리포트 생성 등) */
    public String execute(AgentContext context) {
        if (context.getIntentResult().intentType() == IntentType.SCHEDULE_CONSULTATION) {
            return executeScheduleConsultation(context);
        }
        if (context.getIntentResult().intentType() == IntentType.QUIZ_TOOL) {
            return executeQuizTool(context);
        }

        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(context.getRole()))
                .user(buildUserPrompt(context))
                .call()
                .content();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private Flux<String> executeScheduleConsultationStream(AgentContext context) {
        if (context.getTeacherId() == null) {
            return Flux.just("로그인이 필요합니다.");
        }
        ConsultationScheduleTool registerTool =
                new ConsultationScheduleTool(context.getTeacherId(), scheduleMapper, studentMapper, counselingNoteMapper);
        ScheduleQueryTool queryTool =
                new ScheduleQueryTool(context.getTeacherId(), scheduleMapper);

        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(ChatRole.TEACHER))
                .user(scheduleUserPrompt(context.getUserMessage()))
                .tools(registerTool, queryTool)
                .stream()
                .content();
    }

    private String executeScheduleConsultation(AgentContext context) {
        if (context.getTeacherId() == null) {
            return "로그인이 필요합니다.";
        }
        ConsultationScheduleTool registerTool =
                new ConsultationScheduleTool(context.getTeacherId(), scheduleMapper, studentMapper, counselingNoteMapper);
        ScheduleQueryTool queryTool =
                new ScheduleQueryTool(context.getTeacherId(), scheduleMapper);

        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(ChatRole.TEACHER))
                .user(scheduleUserPrompt(context.getUserMessage()))
                .tools(registerTool, queryTool)
                .call()
                .content();
    }

    private Flux<String> executeQuizToolStream(AgentContext context) {
        if (context.getTeacherId() == null) return Flux.just("로그인이 필요합니다.");
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadRawPrompt(quizSystemPrompt))
                .user(buildQuizUserPrompt(context))
                .tools(buildQuizTools(context.getTeacherId()))
                .stream()
                .content();
    }

    private String executeQuizTool(AgentContext context) {
        if (context.getTeacherId() == null) return "로그인이 필요합니다.";
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadRawPrompt(quizSystemPrompt))
                .user(buildQuizUserPrompt(context))
                .tools(buildQuizTools(context.getTeacherId()))
                .call()
                .content();
    }

    private Object[] buildQuizTools(Long teacherId) {
        return new Object[]{
                new QuizPageFetchTool(teacherId, materialMapper, materialPageMapper),
                new QuizSaveTool(teacherId, quizRepository),
                new QuizQueryTool(teacherId, quizRepository)
        };
    }

    /** 상담 일정 등록 시 현재 날짜를 컨텍스트로 주입 (상대적 날짜 표현 처리용) */
    private String scheduleUserPrompt(String userMessage) {
        return "오늘 날짜: " + LocalDate.now() + "\n\n" + userMessage;
    }

    /**
     * 퀴즈 Tool Calling용 유저 프롬프트.
     * 날짜 + 이전 대화 히스토리 + 현재 메시지를 포함한다.
     * 히스토리가 없으면 첫 요청, 있으면 "발행해줘" 같은 후속 응답 처리가 가능.
     */
    private String buildQuizUserPrompt(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("오늘 날짜: ").append(LocalDate.now()).append("\n\n");
        if (!ctx.getConversationHistory().isBlank()) {
            sb.append("## 이전 대화\n").append(ctx.getConversationHistory()).append("\n\n");
        }
        sb.append("## 요청\n").append(ctx.getUserMessage());
        return sb.toString();
    }

    private String loadSystemPrompt(ChatRole role) {
        Resource resource = role == ChatRole.TEACHER ? teacherSystemPrompt : studentSystemPrompt;
        return loadRawPrompt(resource);
    }

    private String loadRawPrompt(Resource resource) {
        try {
            return new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("시스템 프롬프트 로드 실패", e);
        }
    }

    /**
     * [참고 자료] + [이전 대화] + [질문] 순으로 조합.
     * 각 섹션은 내용이 있을 때만 포함.
     */
    private String buildUserPrompt(AgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        boolean hasContext = !ctx.getRetrievedContext().isBlank();

        if (hasContext) {
            sb.append("## 참고 자료\n").append(ctx.getRetrievedContext()).append("\n\n");
            sb.append("위 참고 자료에 [출처: 파일명 N페이지] 형태로 표시된 경우, 답변 말미에 반드시 출처를 명시하세요. 마크다운 문법은 사용하지 마세요.\n\n");
        } else {
            sb.append("참고할 교재 자료가 없습니다. 일반 교육 지식으로만 답변하고, 출처나 페이지 번호를 절대 임의로 만들지 마세요.\n\n");
        }

        if (!ctx.getConversationHistory().isBlank()) {
            sb.append("## 이전 대화\n").append(ctx.getConversationHistory()).append("\n\n");
        }
        sb.append("## 질문\n").append(ctx.getUserMessage());
        return sb.toString();
    }
}
