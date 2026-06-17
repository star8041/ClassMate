package com.example.myapp.material.service;

import com.example.myapp.material.dto.MaterialFile;
import com.example.myapp.material.dto.MaterialResponse;
import com.example.myapp.material.entity.Material;
import com.example.myapp.material.entity.MaterialPage;
import com.example.myapp.material.mapper.MaterialMapper;
import com.example.myapp.material.mapper.MaterialPageMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 강의자료(PDF) 관리 비즈니스 로직.
 */
@Service
public class MaterialService {

    private static final Logger log = LoggerFactory.getLogger(MaterialService.class);
    private static final char NUL = 0;

    private final MaterialMapper materialMapper;
    private final MaterialPageMapper materialPageMapper;
    private final VectorStore vectorStore;
    private final Path uploadDir;

    public MaterialService(MaterialMapper materialMapper,
                           MaterialPageMapper materialPageMapper,
                           VectorStore vectorStore,
                           @Value("${app.material.upload-dir}") String uploadDir) {
        this.materialMapper = materialMapper;
        this.materialPageMapper = materialPageMapper;
        this.vectorStore = vectorStore;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    @Transactional
    public MaterialResponse upload(MultipartFile file, Long teacherId, String subject, String category) {
        validatePdf(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "material.pdf" : file.getOriginalFilename());
        String storedName = UUID.randomUUID() + ".pdf";
        Path target = uploadDir.resolve(storedName);

        try {
            file.transferTo(target);

            List<String> pageTexts = extractPages(target);

            Material material = Material.builder()
                    .teacherId(teacherId)
                    .fileName(originalName)
                    .subject(subject)
                    .category(normalizeCategory(category))
                    .storagePath(target.toString())
                    .totalPages(pageTexts.size())
                    .build();
            Long id = materialMapper.insert(material);
            material.setMaterialId(id);

            for (int i = 0; i < pageTexts.size(); i++) {
                materialPageMapper.insert(MaterialPage.builder()
                        .materialId(id)
                        .pageNumber(i + 1)
                        .pageText(pageTexts.get(i))
                        .build());
            }

            ingestToVectorStore(material, pageTexts);

            return MaterialResponse.from(materialMapper.findById(id).orElse(material));
        } catch (IOException e) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.", e);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    /** 업로드 카테고리 정규화: LESSON(수업자료)/REFERENCE(참고자료), 미지정 시 LESSON */
    private String normalizeCategory(String category) {
        if (category == null) {
            return "LESSON";
        }
        String c = category.trim().toUpperCase();
        return c.equals("REFERENCE") ? "REFERENCE" : "LESSON";
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(Long teacherId) {
        return materialMapper.findAll(teacherId).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse get(Long materialId) {
        return MaterialResponse.from(getEntityOrThrow(materialId));
    }

    @Transactional(readOnly = true)
    public MaterialFile download(Long materialId) {
        Material material = getEntityOrThrow(materialId);
        Resource resource = new FileSystemResource(material.getStoragePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 파일을 찾을 수 없습니다.");
        }
        return new MaterialFile(material.getFileName(), resource);
    }

    @Transactional(readOnly = true)
    public byte[] downloadAllAsZip(Long teacherId) {
        List<Material> materials = materialMapper.findAll(teacherId);
        if (materials.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "다운로드할 자료가 없습니다.");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Material material : materials) {
                Path path = Paths.get(material.getStoragePath());
                if (!Files.exists(path)) continue;
                zos.putNextEntry(new ZipEntry(material.getMaterialId() + "_" + material.getFileName()));
                Files.copy(path, zos);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ZIP 생성에 실패했습니다.", e);
        }
    }

    @Transactional
    public void delete(Long materialId) {
        Material material = getEntityOrThrow(materialId);
        materialMapper.deleteById(materialId);
        deleteVectorsByMaterialId(materialId);
        deleteQuietly(Paths.get(material.getStoragePath()));
    }

    // ===== 내부 헬퍼 =====

    private List<String> extractPages(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<String> texts = new ArrayList<>(totalPages);
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                texts.add(sanitize(stripper.getText(document)));
            }
            return texts;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 PDF 파일이 아닙니다.", e);
        }
    }

    private void ingestToVectorStore(Material material, List<String> pageTexts) {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < pageTexts.size(); i++) {
            String text = pageTexts.get(i);
            if (!StringUtils.hasText(text)) continue;
            documents.add(Document.builder()
                    .text(text)
                    .metadata("materialId", material.getMaterialId())
                    .metadata("teacherId", material.getTeacherId())
                    .metadata("pageNumber", i + 1)
                    .metadata("fileName", material.getFileName())
                    .build());
        }
        if (documents.isEmpty()) return;
        try {
            vectorStore.add(documents);
            log.info("벡터 저장 완료: materialId={}, documents={}", material.getMaterialId(), documents.size());
        } catch (Exception e) {
            safeDeleteVectors(material.getMaterialId());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "벡터 저장에 실패하여 업로드를 롤백했습니다.", e);
        }
    }

    private void deleteVectorsByMaterialId(Long materialId) {
        Filter.Expression expr = new FilterExpressionBuilder().eq("materialId", materialId).build();
        vectorStore.delete(expr);
    }

    private void safeDeleteVectors(Long materialId) {
        try {
            deleteVectorsByMaterialId(materialId);
        } catch (Exception ignore) {
            log.warn("롤백 중 벡터 정리 실패 (무시). materialId={}", materialId);
        }
    }

    private Material getEntityOrThrow(Long materialId) {
        return materialMapper.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "자료를 찾을 수 없습니다. id=" + materialId));
    }

    /** PostgreSQL text 컬럼이 거부하는 NUL(0x00) 문자를 제거한다. */
    private String sanitize(String text) {
        return text == null ? null : text.replace(String.valueOf(NUL), "");
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 비어 있습니다.");
        }
        String name = file.getOriginalFilename();
        boolean pdfByName = name != null && name.toLowerCase().endsWith(".pdf");
        boolean pdfByType = "application/pdf".equalsIgnoreCase(file.getContentType());
        if (!pdfByName && !pdfByType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF 파일만 업로드할 수 있습니다.");
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제 실패: " + path, e);
        }
    }
}
