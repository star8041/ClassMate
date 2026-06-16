package com.example.myapp.material.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * PDF에서 추출한 페이지별 원문 텍스트. material_page 테이블과 매핑된다.
 */
@Getter
@Setter
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
