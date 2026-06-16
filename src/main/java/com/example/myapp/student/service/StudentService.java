package com.example.myapp.student.service;

import com.example.myapp.student.dto.StudentCreateRequest;
import com.example.myapp.student.dto.StudentResponse;
import com.example.myapp.student.dto.StudentUpdateRequest;
import com.example.myapp.student.entity.Student;
import com.example.myapp.student.mapper.StudentMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 학생 관리 비즈니스 로직.
 * <ul>
 *     <li>조회: 목록 / 단건</li>
 *     <li>수정: 이름 / 학번</li>
 *     <li>삭제</li>
 * </ul>
 */
@Service
public class StudentService {

    private final StudentMapper studentMapper;

    public StudentService(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    /** 학생 등록 (현재 교사에게 소속) */
    @Transactional
    public StudentResponse create(Long teacherId, StudentCreateRequest request) {
        if (!StringUtils.hasText(request.studentName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 이름은 비어 있을 수 없습니다.");
        }

        Student student = Student.builder()
                .teacherId(teacherId)
                .studentName(request.studentName())
                .studentNumber(request.studentNumber())
                .build();

        try {
            Long id = studentMapper.insert(student);
            return StudentResponse.from(getEntityOrThrow(id));
        } catch (DataIntegrityViolationException e) {
            // teacher_id FK 위반 등
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "존재하지 않는 교사입니다. teacherId=" + teacherId, e);
        }
    }

    /** 학생 목록 조회 (teacherId 가 null 이면 전체) */
    @Transactional(readOnly = true)
    public List<StudentResponse> list(Long teacherId) {
        return studentMapper.findAll(teacherId).stream()
                .map(StudentResponse::from)
                .toList();
    }

    /** 학생 상세 조회 */
    @Transactional(readOnly = true)
    public StudentResponse get(Long studentId) {
        return StudentResponse.from(getEntityOrThrow(studentId));
    }

    /** 학생 정보 수정 (이름/학번) */
    @Transactional
    public StudentResponse update(Long studentId, StudentUpdateRequest request) {
        Student student = getEntityOrThrow(studentId);

        if (!StringUtils.hasText(request.studentName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생 이름은 비어 있을 수 없습니다.");
        }

        student.setStudentName(request.studentName());
        student.setStudentNumber(request.studentNumber());
        studentMapper.update(student);

        return StudentResponse.from(getEntityOrThrow(studentId));
    }

    /** 학생 삭제 */
    @Transactional
    public void delete(Long studentId) {
        getEntityOrThrow(studentId); // 존재 확인 (없으면 404)
        studentMapper.deleteById(studentId);
    }

    // ===== 내부 헬퍼 =====

    private Student getEntityOrThrow(Long studentId) {
        return studentMapper.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다. id=" + studentId));
    }
}
