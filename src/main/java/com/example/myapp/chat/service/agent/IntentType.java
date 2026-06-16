package com.example.myapp.chat.service.agent;

public enum IntentType {
    /** 교재 개념 질문 → vector_store 유사도 검색 */
    VECTOR_RAG,
    /** 특정 페이지 번호 기반 질문 → material_page 직접 조회 */
    PAGE_SEARCH,
    /** 교재 무관 일반 질문 → AI 직접 응답 */
    DIRECT
}
