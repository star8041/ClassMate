package com.example.myapp.chat.service.agent;

import com.example.myapp.chat.entity.ChatRole;
import lombok.Builder;
import lombok.Getter;

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
}
