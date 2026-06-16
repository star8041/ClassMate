package com.example.myapp.material.dto;

import org.springframework.core.io.Resource;

/**
 * PDF 다운로드 시 컨트롤러로 전달하는 파일 묶음 DTO.
 * (다운로드용 파일명 + 실제 리소스)
 */
public record MaterialFile(
        String fileName,
        Resource resource
) {
}
