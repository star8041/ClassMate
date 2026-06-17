package com.example.myapp.chat.service;

import com.example.myapp.chat.entity.ChatMessage;
import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.entity.ChatSession;
import com.example.myapp.chat.mapper.ChatMessageMapper;
import com.example.myapp.chat.service.agent.AgentContext;
import com.example.myapp.chat.service.agent.AgentExecutor;
import com.example.myapp.chat.service.agent.SupervisorAgent;
import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import com.example.myapp.material.entity.Material;
import com.example.myapp.material.mapper.MaterialMapper;
import com.example.myapp.student.entity.Student;
import com.example.myapp.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentChatService {

    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper chatMessageMapper;
    private final SupervisorAgent supervisorAgent;
    private final AgentExecutor agentExecutor;
    private final StudentMapper studentMapper;
    private final MaterialMapper materialMapper;

    @Transactional
    public Flux<String> sendMessage(Long studentId, Long sessionId, String messageText) {
        ChatSession session = chatSessionService.getOwnedSession(sessionId, studentId);

        Student student = studentMapper.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));
        List<Long> teacherMaterialIds = materialMapper.findAll(student.getTeacherId())
                .stream().map(Material::getMaterialId).toList();

        saveMessage(sessionId, ChatRole.STUDENT, messageText);

        AgentContext context = supervisorAgent.analyze(
                messageText, ChatRole.STUDENT, sessionId, session.getMaterialId(), teacherMaterialIds);

        StringBuilder fullResponse = new StringBuilder();
        return agentExecutor.executeStream(context)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> saveMessage(sessionId, ChatRole.AI, fullResponse.toString()))
                .doOnError(e -> log.error("[StudentChat] 스트리밍 오류 sessionId={}", sessionId, e));
    }

    private void saveMessage(Long sessionId, ChatRole role, String text) {
        chatMessageMapper.insert(ChatMessage.builder()
                .chatSessionId(sessionId)
                .role(role)
                .messageText(text)
                .build());
    }
}
