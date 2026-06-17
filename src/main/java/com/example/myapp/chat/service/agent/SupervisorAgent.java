package com.example.myapp.chat.service.agent;

import com.example.myapp.chat.entity.ChatRole;
import com.example.myapp.chat.mapper.ChatMessageMapper;
import com.example.myapp.material.entity.MaterialPage;
import com.example.myapp.material.mapper.MaterialPageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Supervisor Agent.
 *
 * 흐름:
 *   1. classifyIntent()  — AI로 의도 분류 (IntentType)
 *   2. retrieveContext() — 의도에 따라 vector_store 또는 material_page 검색
 *   3. buildHistory()    — 최근 대화 이력 직렬화
 *   4. AgentContext 반환 → AgentExecutor가 응답 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupervisorAgent {

    private static final int HISTORY_LIMIT = 10;
    private static final int VECTOR_TOP_K = 5;
    private static final double VECTOR_THRESHOLD = 0.50;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final MaterialPageMapper materialPageMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Value("classpath:prompts/intent-classifier.st")
    private Resource intentClassifierPrompt;

    /** 교사 채팅용 (teacherMaterialIds 없음) */
    public AgentContext analyze(String userMessage, ChatRole role, Long sessionId, Long materialId) {
        return analyze(userMessage, role, sessionId, materialId, Collections.emptyList());
    }

    /** 학생 채팅용 (담임선생님 자료 ID 목록 포함) */
    public AgentContext analyze(String userMessage, ChatRole role, Long sessionId,
                                Long materialId, List<Long> teacherMaterialIds) {
        IntentResult intentResult = classifyIntent(userMessage, materialId != null || !teacherMaterialIds.isEmpty());
        log.info("[Supervisor] sessionId={} intent={} reason={}",
                sessionId, intentResult.intentType(), intentResult.reasoning());

        String context = retrieveContext(intentResult, materialId, teacherMaterialIds);
        String history = buildHistory(sessionId);

        return AgentContext.builder()
                .role(role)
                .intentResult(intentResult)
                .retrievedContext(context)
                .conversationHistory(history)
                .userMessage(userMessage)
                .sessionId(sessionId)
                .materialId(materialId)
                .teacherMaterialIds(teacherMaterialIds)
                .build();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private IntentResult classifyIntent(String userMessage, boolean hasMaterial) {
        try {
            String prompt = new String(intentClassifierPrompt.getContentAsByteArray(), StandardCharsets.UTF_8)
                    .replace("{userMessage}", userMessage)
                    .replace("{hasMaterial}", hasMaterial ? "true" : "false");

            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(IntentResult.class);
        } catch (Exception e) {
            log.error("[Supervisor] Intent classification failed", e);
            return new IntentResult(IntentType.DIRECT, null, null, null, "fallback to DIRECT");
        }
    }

    private String retrieveContext(IntentResult intent, Long materialId, List<Long> teacherMaterialIds) {
        return switch (intent.intentType()) {

            case VECTOR_RAG -> {
                if (intent.searchQuery() == null || intent.searchQuery().isBlank()) yield "";

                SearchRequest.Builder req = SearchRequest.builder()
                        .query(intent.searchQuery())
                        .topK(VECTOR_TOP_K)
                        .similarityThreshold(VECTOR_THRESHOLD);

                if (materialId != null) {
                    req.filterExpression("materialId == " + materialId);
                } else if (teacherMaterialIds != null && !teacherMaterialIds.isEmpty()) {
                    if (teacherMaterialIds.size() == 1) {
                        req.filterExpression("materialId == " + teacherMaterialIds.get(0));
                    } else {
                        req.filterExpression("materialId in " + teacherMaterialIds);
                    }
                }

                log.info("[Supervisor] VECTOR_RAG 검색 시작 - query='{}' materialId={} teacherMaterialIds={} threshold={}",
                        intent.searchQuery(), materialId, teacherMaterialIds, VECTOR_THRESHOLD);

                List<Document> docs = vectorStore.similaritySearch(req.build());

                log.info("[Supervisor] VECTOR_RAG 검색 결과 - {}건 발견", docs.size());
                docs.forEach(doc -> log.debug("[Supervisor]   └ fileName={} page={} score={}",
                        doc.getMetadata().get("fileName"),
                        doc.getMetadata().get("pageNumber"),
                        doc.getMetadata().get("distance")));

                yield docs.stream()
                        .map(doc -> {
                            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "");
                            Object pageObj = doc.getMetadata().get("pageNumber");
                            String header = "";
                            if (!fileName.isBlank() && pageObj != null) {
                                header = "[출처: " + fileName + " " + pageObj + "페이지]\n";
                            } else if (!fileName.isBlank()) {
                                header = "[출처: " + fileName + "]\n";
                            }
                            return header + doc.getText();
                        })
                        .collect(Collectors.joining("\n\n---\n\n"));
            }

            case PAGE_SEARCH -> {
                if (materialId == null) yield "";

                List<MaterialPage> pages;
                if (intent.startPage() != null && intent.endPage() != null) {
                    pages = materialPageMapper.findByPageRange(
                            materialId, intent.startPage(), intent.endPage());
                } else if (intent.searchQuery() != null && !intent.searchQuery().isBlank()) {
                    pages = materialPageMapper.searchByKeyword(materialId, intent.searchQuery());
                } else {
                    yield "";
                }

                yield pages.stream()
                        .map(p -> "[%d페이지]\n%s".formatted(p.getPageNumber(), p.getPageText()))
                        .collect(Collectors.joining("\n\n---\n\n"));
            }

            case DIRECT -> "";
        };
    }

    private String buildHistory(Long sessionId) {
        return chatMessageMapper.findRecentMessages(sessionId, HISTORY_LIMIT).stream()
                .map(m -> "%s: %s".formatted(m.getRole().name(), m.getMessageText()))
                .collect(Collectors.joining("\n"));
    }
}
