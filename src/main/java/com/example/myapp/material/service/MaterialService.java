package com.example.myapp.material.service;

import com.example.myapp.material.dto.MaterialFile;
import com.example.myapp.material.dto.MaterialResponse;
import com.example.myapp.material.entity.Material;
import com.example.myapp.material.mapper.MaterialMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 강의자료(PDF) 관리 비즈니스 로직.
 * <ul>
 *     <li>업로드: 서버 디스크에 저장 + PDFBox로 페이지 수 추출 + DB 기록</li>
 *     <li>조회: 목록 / 단건</li>
 *     <li>다운로드: 단건 / 전체(ZIP)</li>
 *     <li>삭제: DB 행 + 실제 파일 함께 제거</li>
 * </ul>
 */
@Service
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final Path uploadDir;

    public MaterialService(MaterialMapper materialMapper,
                           @Value("${app.material.upload-dir}") String uploadDir) {
        this.materialMapper = materialMapper;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 애플리케이션 기동 시 업로드 디렉터리를 보장한다. */
    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadDir, e);
        }
    }

    /**
     * PDF 업로드: 파일을 저장하고 페이지 수를 추출한 뒤 메타정보를 DB에 기록한다.
     */
    @Transactional
    public MaterialResponse upload(MultipartFile file, Long teacherId, String subject) {
        validatePdf(file);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "material.pdf" : file.getOriginalFilename());
        // 충돌 방지를 위해 저장 파일명은 UUID 사용
        String storedName = UUID.randomUUID() + ".pdf";
        Path target = uploadDir.resolve(storedName);

        try {
            file.transferTo(target);

            int totalPages = countPages(target);

            Material material = Material.builder()
                    .teacherId(teacherId)
                    .fileName(originalName)
                    .subject(subject)
                    .storagePath(target.toString())
                    .totalPages(totalPages)
                    .build();

            Long id = materialMapper.insert(material);
            material.setMaterialId(id);
            return MaterialResponse.from(materialMapper.findById(id).orElse(material));
        } catch (IOException e) {
            // 저장 실패 시 디스크에 남은 파일 정리
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.", e);
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    /** PDF 목록 조회 (teacherId 가 null 이면 전체) */
    @Transactional(readOnly = true)
    public List<MaterialResponse> list(Long teacherId) {
        return materialMapper.findAll(teacherId).stream()
                .map(MaterialResponse::from)
                .toList();
    }

    /** PDF 상세(메타) 조회 */
    @Transactional(readOnly = true)
    public MaterialResponse get(Long materialId) {
        return MaterialResponse.from(getEntityOrThrow(materialId));
    }

    /** 단일 PDF 다운로드용 파일 로드 */
    @Transactional(readOnly = true)
    public MaterialFile download(Long materialId) {
        Material material = getEntityOrThrow(materialId);
        Resource resource = new FileSystemResource(material.getStoragePath());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "저장된 파일을 찾을 수 없습니다.");
        }
        return new MaterialFile(material.getFileName(), resource);
    }

    /**
     * 전체 PDF를 ZIP으로 묶어 반환한다. (teacherId 가 null 이면 전체 자료 대상)
     * 동일 파일명 충돌을 막기 위해 ZIP 내부 이름에 materialId 를 접두한다.
     */
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
                if (!Files.exists(path)) {
                    continue; // 디스크에서 사라진 파일은 건너뜀
                }
                String entryName = material.getMaterialId() + "_" + material.getFileName();
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(path, zos);
                zos.closeEntry();
            }
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ZIP 생성에 실패했습니다.", e);
        }
    }

    /** PDF 삭제: DB 행과 실제 파일을 함께 제거한다. */
    @Transactional
    public void delete(Long materialId) {
        Material material = getEntityOrThrow(materialId);
        materialMapper.deleteById(materialId);
        deleteQuietly(Paths.get(material.getStoragePath()));
    }

    // ===== 내부 헬퍼 =====

    private Material getEntityOrThrow(Long materialId) {
        return materialMapper.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "자료를 찾을 수 없습니다. id=" + materialId));
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

    private int countPages(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 PDF 파일이 아닙니다.", e);
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
