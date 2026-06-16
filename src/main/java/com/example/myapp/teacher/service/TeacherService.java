package com.example.myapp.teacher.service;

import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import com.example.myapp.teacher.dto.TeacherResponse;
import com.example.myapp.teacher.dto.TeacherSignupRequest;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 교사 계정 비즈니스 로직 (회원가입).
 */
@Service
public class TeacherService {

    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;

    public TeacherService(TeacherMapper teacherMapper, PasswordEncoder passwordEncoder) {
        this.teacherMapper = teacherMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 선생님 회원가입: 비밀번호를 BCrypt 로 해시하여 저장한다. */
    @Transactional
    public TeacherResponse signup(TeacherSignupRequest request) {
        if (!StringUtils.hasText(request.loginId())
                || !StringUtils.hasText(request.password())
                || !StringUtils.hasText(request.teacherName())) {
            throw new BusinessException(ErrorCode.SIGNUP_INVALID);
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        if (teacherMapper.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        Teacher teacher = Teacher.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .teacherName(request.teacherName())
                .email(request.email())
                // DDL NOT NULL 제약을 위해 미입력 시 빈 문자열
                .question(request.question() == null ? "" : request.question())
                .answer(request.answer() == null ? "" : request.answer())
                // 자가 회원가입은 항상 일반 교사(USER). 관리자(ADMIN)는 시더로만 생성.
                .role("USER")
                .build();

        Long id = teacherMapper.insert(teacher);
        teacher.setTeacherId(id);
        return TeacherResponse.from(teacher);
    }

    /** 전체 교사 목록 (관리자용) */
    @Transactional(readOnly = true)
    public java.util.List<TeacherResponse> listAll() {
        return teacherMapper.findAll().stream()
                .map(TeacherResponse::from)
                .toList();
    }
}
