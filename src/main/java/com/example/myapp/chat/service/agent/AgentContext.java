package com.example.myapp.chat.service.agent;

import com.example.myapp.chat.entity.ChatRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * SupervisorAgent → AgentExecutor 전달 컨테이너.
 */
@Getter
@Builder
public class AgentContext {

    private final ChatRole role;
    private final IntentResult intentResult;
    private final String retrievedContext;
    private final String conversationHistory;
    private final String userMessage;
    private final Long sessionId;
    private final Long materialId;

    /**
     * 학생 채팅 시 담임선생님의 자료 ID 목록.
     * VECTOR_RAG 필터링에 사용 (materialId가 null일 때 선생님 자료로 범위 한정).
     */
    private final List<Long> teacherMaterialIds;

    /** 교사 채팅 시 teacherId (Tool Calling용) */
    private final Long teacherId;
}
