package com.example.myapp.chat.service.agent;

import com.example.myapp.chat.entity.ChatRole;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final ChatModel chatModel;

    @Value("classpath:prompts/teacher-system.st")
    private Resource teacherSystemPrompt;

    @Value("classpath:prompts/student-system.st")
    private Resource studentSystemPrompt;

    /** SSE 스트리밍 응답 */
    public Flux<String> executeStream(AgentContext context) {
        log.debug("[Executor] role={} intent={}", context.getRole(), context.getIntentResult().intentType());
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(context.getRole()))
                .user(buildUserPrompt(context))
                .stream()
                .content();
    }

    /** 논-스트리밍 단건 호출 (리포트 생성 등) */
    public String execute(AgentContext context) {
        return ChatClient.builder(chatModel).build()
                .prompt()
                .system(loadSystemPrompt(context.getRole()))
                .user(buildUserPrompt(context))
                .call()
                .content();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private String loadSystemPrompt(ChatRole role) {
        Resource resource = role == ChatRole.TEACHER ? teacherSystemPrompt : studentSystemPrompt;
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
            sb.append("위 참고 자료에 [출처: 파일명 N페이지] 형태로 표시된 경우, 답변 말미에 반드시 출처를 명시하세요.\n\n");
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
