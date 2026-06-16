package com.example.myapp.material.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 교사가 업로드한 교과서 PDF의 원본 정보를 나타내는 엔티티.
 * material 테이블과 1:1 매핑된다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    /** PK (BIGSERIAL) */
    private Long materialId;

    /** 업로드한 교사 ID (FK) */
    private Long teacherId;

    /** 원본 파일명 (사용자가 올린 이름) */
    private String fileName;

    /** 과목 (선택) */
    private String subject;

    /** 서버에 저장된 실제 파일 경로 */
    private String storagePath;

    /** PDF 전체 페이지 수 */
    private int totalPages;

    /** 업로드 시각 */
    private LocalDateTime uploadedAt;
}
