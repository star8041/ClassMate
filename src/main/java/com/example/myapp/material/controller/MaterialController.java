package com.example.myapp.material.controller;

import com.example.myapp.material.dto.MaterialFile;
import com.example.myapp.material.dto.MaterialResponse;
import com.example.myapp.material.service.MaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 강의자료(PDF) 관리 REST API. (PDF 명세서 기준)
 *
 * <pre>
 * POST   /api/v1/materials                       PDF 업로드
 * GET    /api/v1/materials                       PDF 목록 조회
 * GET    /api/v1/materials/{materialId}          PDF 상세 조회
 * GET    /api/v1/materials/{materialId}/download PDF 다운로드
 * GET    /api/v1/materials/download/all          PDF 전체 다운로드(ZIP)
 * DELETE /api/v1/materials/{materialId}          PDF 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /** PDF 업로드 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("teacherId") Long teacherId,
            @RequestParam(value = "subject", required = false) String subject) {
        return materialService.upload(file, teacherId, subject);
    }

    /** PDF 목록 조회 (teacherId 미지정 시 전체) */
    @GetMapping
    public List<MaterialResponse> list(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return materialService.list(teacherId);
    }

    /** PDF 전체 다운로드 (ZIP) */
    @GetMapping("/download/all")
    public ResponseEntity<byte[]> downloadAll(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        byte[] zip = materialService.downloadAllAsZip(teacherId);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("materials.zip"))
                .body(zip);
    }

    /** PDF 상세 조회 */
    @GetMapping("/{materialId}")
    public MaterialResponse detail(@PathVariable("materialId") Long materialId) {
        return materialService.get(materialId);
    }

    /** PDF 단건 다운로드 */
    @GetMapping("/{materialId}/download")
    public ResponseEntity<Resource> download(@PathVariable("materialId") Long materialId) {
        MaterialFile file = materialService.download(materialId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(file.fileName()))
                .body(file.resource());
    }

    /** PDF 삭제 */
    @DeleteMapping("/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("materialId") Long materialId) {
        materialService.delete(materialId);
    }

    /** 한글 파일명을 위해 RFC 5987 인코딩으로 Content-Disposition 헤더를 만든다. */
    private String attachment(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
