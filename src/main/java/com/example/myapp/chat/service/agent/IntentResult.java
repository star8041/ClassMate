package com.example.myapp.chat.service.agent;

/**
 * Spring AI structured output으로 역직렬화되는 의도 분류 결과.
 */
public record IntentResult(
        IntentType intentType,
        String searchQuery,
        Integer startPage,
        Integer endPage,
        String reasoning
) {}
