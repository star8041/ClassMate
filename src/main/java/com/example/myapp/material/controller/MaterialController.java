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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 * 강의자료(PDF) 관리 REST API.
 *
 * <pre>
 * POST   /api/materials               PDF 업로드
 * GET    /api/materials               PDF 목록 조회
 * GET    /api/materials/{id}          PDF 상세 조회
 * GET    /api/materials/{id}/download PDF 다운로드
 * GET    /api/materials/download      PDF 전체 다운로드(ZIP)
 * POST   /api/materials/{id}/delete   PDF 삭제
 * </pre>
 */
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /** PDF 업로드 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse upload(
            @AuthenticationPrincipal Long teacherId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "category", required = false) String category) {
        return materialService.upload(file, teacherId, subject, category);
    }

    /** PDF 목록 조회 (본인 자료만) */
    @GetMapping
    public List<MaterialResponse> list(@AuthenticationPrincipal Long teacherId) {
        return materialService.list(teacherId);
    }

    /** 학생용 PDF 목록 조회 (특정 교사의 자료, 인증 불필요. teacherId 생략 시 전체) */
    @GetMapping("/public")
    public List<MaterialResponse> listPublic(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        return materialService.list(teacherId);
    }

    /** PDF 상세 조회 */
    @GetMapping("/{id}")
    public MaterialResponse detail(@PathVariable("id") Long id) {
        return materialService.get(id);
    }

    /** PDF 단건 다운로드 */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id) {
        MaterialFile file = materialService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(file.fileName()))
                .body(file.resource());
    }

    /** PDF 인라인 보기 (브라우저에서 바로 열람) */
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable("id") Long id) {
        MaterialFile file = materialService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, inline(file.fileName()))
                .body(file.resource());
    }

    /** PDF 전체 다운로드 (ZIP) */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadAll(
            @RequestParam(value = "teacherId", required = false) Long teacherId) {
        byte[] zip = materialService.downloadAllAsZip(teacherId);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("materials.zip"))
                .body(zip);
    }

    /** PDF 삭제 (스펙에 따라 POST) */
    @PostMapping("/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        materialService.delete(id);
    }

    /** 한글 파일명을 위해 RFC 5987 인코딩으로 Content-Disposition 헤더를 만든다. */
    private String attachment(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    /** 인라인 보기용 Content-Disposition (브라우저 내 PDF 뷰어로 표시) */
    private String inline(String fileName) {
        return ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
