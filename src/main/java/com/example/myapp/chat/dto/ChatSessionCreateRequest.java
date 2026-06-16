package com.example.myapp.chat.dto;

public record ChatSessionCreateRequest(
        Long materialId,
        String title
) {}
