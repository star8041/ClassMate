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
 * 퀴즈 생성 흐름에서 첫 번째로 호출됨:
 *   1. QuizPageFetchTool → 페이지 텍스트 반환
 *   2. LLM이 텍스트를 바탕으로 문제 생성
 *   3. QuizSaveTool → DB 저장
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
     * 교재 키워드와 페이지 범위로 텍스트를 가져온다.
     *
     * @param materialKeyword 교재 파일명 키워드 (예: "과학교과서", "수학")
     * @param startPage       시작 페이지 번호
     * @param endPage         종료 페이지 번호
     */
    @Tool(description = "교사가 업로드한 PDF 교재에서 특정 페이지 범위의 내용을 가져옵니다. 퀴즈 생성 전에 먼저 호출하세요.")
    public String fetchMaterialPages(
            @ToolParam(description = "교재 파일명 키워드 (예: '과학교과서', '수학', 'science')") String materialKeyword,
            @ToolParam(description = "시작 페이지 번호 (예: 10)") int startPage,
            @ToolParam(description = "종료 페이지 번호 (예: 15)") int endPage) {

        log.info("[QuizPageFetchTool] teacherId={} keyword={} pages={}-{}", teacherId, materialKeyword, startPage, endPage);

        List<Material> materials = materialMapper.findByTeacherAndKeyword(teacherId, materialKeyword);
        if (materials.isEmpty()) {
            return "키워드 '" + materialKeyword + "'에 해당하는 교재를 찾을 수 없습니다. " +
                    "업로드된 교재 목록을 확인하거나 다른 키워드로 시도해보세요.";
        }

        // 가장 최근에 업로드된 교재 사용
        Material material = materials.get(0);
        log.info("[QuizPageFetchTool] materialId={} fileName={}", material.getMaterialId(), material.getFileName());

        List<MaterialPage> pages = materialPageMapper.findByPageRange(material.getMaterialId(), startPage, endPage);
        if (pages.isEmpty()) {
            return "교재 '" + material.getFileName() + "'의 " + startPage + "~" + endPage +
                    "페이지 내용을 찾을 수 없습니다. (전체 페이지: " + material.getTotalPages() + "쪽)";
        }

        String pageText = pages.stream()
                .map(p -> "[%d페이지]\n%s".formatted(p.getPageNumber(), p.getPageText()))
                .collect(Collectors.joining("\n\n"));

        return ("materialId: %d\n파일명: %s\n조회 페이지: %d~%d쪽\n\n%s")
                .formatted(material.getMaterialId(), material.getFileName(), startPage, endPage, pageText);
    }
}
