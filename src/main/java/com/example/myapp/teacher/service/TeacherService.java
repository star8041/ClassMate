package com.example.myapp.teacher.service;

import com.example.myapp.teacher.dto.TeacherResponse;
import com.example.myapp.teacher.dto.TeacherSignupRequest;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름/아이디/비밀번호는 필수입니다.");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (teacherMapper.existsByLoginId(request.loginId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }

        Teacher teacher = Teacher.builder()
                .loginId(request.loginId())
                .password(passwordEncoder.encode(request.password()))
                .teacherName(request.teacherName())
                .email(request.email())
                // DDL NOT NULL 제약을 위해 미입력 시 빈 문자열
                .question(request.question() == null ? "" : request.question())
                .answer(request.answer() == null ? "" : request.answer())
                .build();

        Long id = teacherMapper.insert(teacher);
        teacher.setTeacherId(id);
        return TeacherResponse.from(teacher);
    }
}
