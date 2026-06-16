package com.example.myapp.auth.service;

import com.example.myapp.auth.dto.LoginRequest;
import com.example.myapp.auth.dto.TokenResponse;
import com.example.myapp.auth.jwt.JwtTokenProvider;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증 비즈니스 로직 (교사 로그인).
 */
@Service
public class AuthService {

    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(TeacherMapper teacherMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.teacherMapper = teacherMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** 선생님 로그인: 아이디/비밀번호 검증 후 JWT 발급 */
    public TokenResponse loginTeacher(LoginRequest request) {
        Teacher teacher = teacherMapper.findByLoginId(request.loginId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), teacher.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // teacher 테이블에 role 컬럼이 없어 우선 TEACHER 로 고정 (TODO: role 컬럼 도입 시 반영)
        String token = jwtTokenProvider.createToken(teacher.getLoginId(), "TEACHER");
        return TokenResponse.bearer(token);
    }
}
