package com.example.myapp.chat.service.agent;

public enum IntentType {
    /** 교재 개념 질문 → vector_store 유사도 검색 */
    VECTOR_RAG,
    /** 특정 페이지 번호 기반 질문 → material_page 직접 조회 */
    PAGE_SEARCH,
    /** 교재 무관 일반 질문 → AI 직접 응답 */
    DIRECT,
    /** 교사가 학부모/학생 상담 일정 등록을 요청 → Tool Calling으로 DB 등록 */
    SCHEDULE_CONSULTATION,
    /** 교사가 퀴즈 생성 또는 퀴즈 현황 조회를 요청 → Tool Calling으로 DB 생성/조회 */
    QUIZ_TOOL
}
