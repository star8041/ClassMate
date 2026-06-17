package com.example.myapp.material.dto;

import com.example.myapp.material.entity.Material;

import java.time.LocalDateTime;

/**
 * PDF 강의자료 조회 응답 DTO.
 * 내부 저장 경로(storagePath)는 노출하지 않는다.
 */
public record MaterialResponse(
        Long materialId,
        Long teacherId,
        String fileName,
        String subject,
        String category,
        int totalPages,
        LocalDateTime uploadedAt
) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.getMaterialId(),
                material.getTeacherId(),
                material.getFileName(),
                material.getSubject(),
                material.getCategory(),
                material.getTotalPages(),
                material.getUploadedAt()
        );
    }
}
