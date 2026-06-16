package com.example.myapp.material.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PDF에서 추출한 페이지별 텍스트를 나타내는 엔티티.
 * material_page 테이블과 1:1 매핑된다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialPage {

    private Long materialPageId;
    private Long materialId;
    private int pageNumber;
    private String pageText;
    private LocalDateTime createdAt;
}
