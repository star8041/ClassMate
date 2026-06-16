package com.example.myapp.auth.service;

import com.example.myapp.auth.dto.InviteCodeResponse;
import com.example.myapp.auth.dto.LoginRequest;
import com.example.myapp.auth.dto.TokenResponse;
import com.example.myapp.auth.dto.VerifyCodeResponse;
import com.example.myapp.auth.invite.InviteCodeStore;
import com.example.myapp.auth.jwt.JwtTokenProvider;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증 비즈니스 로직 (교사 로그인 / 토큰 기반 본인 식별 / 초대코드).
 */
@Service
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final InviteCodeStore inviteCodeStore;

    public AuthService(TeacherMapper teacherMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       InviteCodeStore inviteCodeStore) {
        this.teacherMapper = teacherMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.inviteCodeStore = inviteCodeStore;
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

    /** Authorization 헤더(Bearer JWT)에서 현재 로그인한 교사를 식별한다. */
    public Teacher getAuthenticatedTeacher(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        String loginId;
        try {
            loginId = jwtTokenProvider.getSubject(token);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", e);
        }
        return teacherMapper.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));
    }

    /** 현재 교사용 임시 초대코드를 생성한다. */
    public InviteCodeResponse generateInviteCode(String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        InviteCodeStore.IssuedCode issued = inviteCodeStore.generate(teacher.getTeacherId());
        return new InviteCodeResponse(issued.code(), issued.expiresAt());
    }

    /** 학생이 제출한 초대코드를 검증하고 연결될 교사 정보를 반환한다. */
    public VerifyCodeResponse verifyInviteCode(String code) {
        Long teacherId = inviteCodeStore.verify(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 초대 코드입니다."));

        Teacher teacher = teacherMapper.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "초대 코드에 연결된 교사를 찾을 수 없습니다."));

        return new VerifyCodeResponse(true, teacher.getTeacherId(), teacher.getTeacherName());
    }
}
