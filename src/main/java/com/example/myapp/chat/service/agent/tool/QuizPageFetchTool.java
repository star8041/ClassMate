package com.example.myapp.chat.service.agent.tool;

import com.example.myapp.material.entity.Material;
import com.example.myapp.material.entity.MaterialPage;
import com.example.myapp.material.mapper.MaterialMapper;
import com.example.myapp.material.mapper.MaterialPageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool Calling: 교사가 업로드한 PDF 교재에서 특정 페이지 범위의 텍스트를 조회한다.
 *
 * 권장 퀴즈 생성 흐름:
 *   1. listAvailableMaterials → 교재 목록 + materialId 확인
 *   2. fetchMaterialPagesByMaterialId(materialId, start, end) → 페이지 텍스트
 *   3. LLM이 텍스트를 바탕으로 문제 생성
 *   4. QuizSaveTool → DB 저장
 */
@Slf4j
public class QuizPageFetchTool {

    private final Long teacherId;
    private final MaterialMapper materialMapper;
    private final MaterialPageMapper materialPageMapper;

    public QuizPageFetchTool(Long teacherId, MaterialMapper materialMapper, MaterialPageMapper materialPageMapper) {
        this.teacherId = teacherId;
        this.materialMapper = materialMapper;
        this.materialPageMapper = materialPageMapper;
    }

    /**
     * 교사가 업로드한 교재 목록(materialId 포함)을 반환한다.
     * 퀴즈 생성 시 항상 이 도구를 먼저 호출해 materialId를 확인하라.
     */
    @Tool(description = "교사가 업로드한 교재(PDF) 목록을 조회합니다. materialId, 파일명, 전체 페이지 수를 반환합니다. 퀴즈 생성 시 가장 먼저 호출하세요.")
    public String listAvailableMaterials() {
        log.info("[QuizPageFetchTool] listAvailableMaterials teacherId={}", teacherId);
        List<Material> materials = materialMapper.findAll(teacherId);
        if (materials.isEmpty()) {
            return "업로드된 교재가 없습니다. 먼저 PDF 교재를 업로드해 주세요.";
        }
        String list = materials.stream()
                .map(m -> "materialId=%d | 파일명=%s | 전체페이지=%d쪽"
                        .formatted(m.getMaterialId(), m.getFileName(), m.getTotalPages()))
                .collect(Collectors.joining("\n"));
        return "업로드된 교재 목록:\n" + list;
    }

    /**
     * materialId로 직접 페이지 텍스트를 조회한다.
     * listAvailableMaterials 결과에서 materialId를 확인한 뒤 이 도구를 사용하라.
     * endPage=0 이면 전체 페이지를 조회한다.
     */
    @Tool(description = "materialId로 교재 페이지 텍스트를 조회합니다. listAvailableMaterials로 materialId를 확인한 뒤 이 도구를 호출하세요. endPage=0이면 전체 페이지를 조회합니다.")
    public String fetchMaterialPagesByMaterialId(
            @ToolParam(description = "교재 ID (listAvailableMaterials 결과에서 확인한 materialId 값)") long materialId,
            @ToolParam(description = "시작 페이지 (1 이상). 전체 조회 시 1을 입력하세요.") int startPage,
            @ToolParam(description = "종료 페이지. 0을 입력하면 전체 페이지를 조회합니다.") int endPage) {

        log.info("[QuizPageFetchTool] fetchByMaterialId materialId={} pages={}-{}", materialId, startPage, endPage);

        Material material = materialMapper.findById(materialId).orElse(null);
        if (material == null || !material.getTeacherId().equals(teacherId)) {
            return "materialId=" + materialId + "에 해당하는 교재를 찾을 수 없습니다. listAvailableMaterials로 올바른 materialId를 확인하세요.";
        }

        int resolvedEnd = (endPage <= 0) ? material.getTotalPages() : endPage;
        int resolvedStart = Math.max(1, startPage);

        List<MaterialPage> pages = materialPageMapper.findByPageRange(materialId, resolvedStart, resolvedEnd);
        if (pages.isEmpty()) {
            return "교재 '" + material.getFileName() + "'의 " + resolvedStart + "~" + resolvedEnd +
                    "페이지 내용을 찾을 수 없습니다. (전체 페이지: " + material.getTotalPages() + "쪽)\n" +
                    "다른 페이지 범위로 시도해 보세요.";
        }

        String pageText = pages.stream()
                .map(p -> "[%d페이지]\n%s".formatted(p.getPageNumber(), p.getPageText()))
                .collect(Collectors.joining("\n\n"));

        return ("materialId: %d\n파일명: %s\n조회 페이지: %d~%d쪽 (전체 %d쪽)\n\n%s")
                .formatted(materialId, material.getFileName(),
                        resolvedStart, resolvedEnd, material.getTotalPages(), pageText);
    }

    /**
     * 키워드로 교재를 검색하고 페이지를 조회한다 (레거시 호환용).
     * 가능하면 listAvailableMaterials + fetchMaterialPagesByMaterialId 조합을 사용할 것.
     */
    @Tool(description = "교재 파일명 키워드로 검색하여 페이지 텍스트를 조회합니다. 가능하면 listAvailableMaterials로 materialId를 먼저 확인한 뒤 fetchMaterialPagesByMaterialId를 사용하세요.")
    public String fetchMaterialPages(
            @ToolParam(description = "교재 파일명 키워드 (예: '과학', '수학')") String materialKeyword,
            @ToolParam(description = "시작 페이지 (1 이상)") int startPage,
            @ToolParam(description = "종료 페이지. 0이면 전체 페이지 조회") int endPage) {

        log.info("[QuizPageFetchTool] fetchByKeyword teacherId={} keyword={} pages={}-{}", teacherId, materialKeyword, startPage, endPage);

        List<Material> materials = materialMapper.findByTeacherAndKeyword(teacherId, materialKeyword);
        if (materials.isEmpty()) {
            String availableList = listAvailableMaterials();
            return "키워드 '" + materialKeyword + "'에 해당하는 교재를 찾을 수 없습니다.\n\n" + availableList
                    + "\n\n위 목록의 materialId를 사용하여 fetchMaterialPagesByMaterialId를 호출하세요.";
        }

        Material material = materials.get(0);
        return fetchMaterialPagesByMaterialId(material.getMaterialId(), startPage, endPage);
    }
}
