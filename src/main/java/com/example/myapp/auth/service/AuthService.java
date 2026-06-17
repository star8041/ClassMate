package com.example.myapp.auth.service;

import com.example.myapp.auth.dto.InviteCodeResponse;
import com.example.myapp.auth.dto.LoginRequest;
import com.example.myapp.auth.dto.StudentLoginRequest;
import com.example.myapp.auth.dto.TokenResponse;
import com.example.myapp.auth.dto.VerifyCodeResponse;
import com.example.myapp.auth.invite.InviteCodeStore;
import com.example.myapp.auth.jwt.JwtTokenProvider;
import com.example.myapp.common.exception.BusinessException;
import com.example.myapp.common.exception.ErrorCode;
import com.example.myapp.student.entity.Student;
import com.example.myapp.student.mapper.StudentMapper;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import io.jsonwebtoken.JwtException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 인증 비즈니스 로직 (교사 로그인 / 토큰 기반 본인 식별 / 관리자 인가 / 초대코드).
 */
@Service
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final InviteCodeStore inviteCodeStore;

    public AuthService(TeacherMapper teacherMapper,
                       StudentMapper studentMapper,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       InviteCodeStore inviteCodeStore) {
        this.teacherMapper = teacherMapper;
        this.studentMapper = studentMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.inviteCodeStore = inviteCodeStore;
    }

    /** 선생님 로그인: 아이디/비밀번호 검증 후 JWT 발급 (role 클레임 포함) */
    public TokenResponse loginTeacher(LoginRequest request) {
        Teacher teacher = teacherMapper.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), teacher.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 실제 권한(USER/ADMIN)을 JWT role 클레임에 반영 (ADMIN 이면 관리자 화면 분기/메뉴 표시)
        String role = teacher.getRole() == null ? "USER" : teacher.getRole();
        String token = jwtTokenProvider.createToken(teacher.getLoginId(), role, teacher.getTeacherName());
        return TokenResponse.bearer(token);
    }

    /** Authorization 헤더(Bearer JWT)에서 현재 로그인한 교사를 식별한다. */
    public Teacher getAuthenticatedTeacher(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        String loginId;
        try {
            loginId = jwtTokenProvider.getSubject(token);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return teacherMapper.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 현재 사용자가 관리자(ADMIN)인지 확인하고 교사 엔티티를 반환한다.
     * 인증 안 됨 → 401, 관리자 아님 → 403.
     */
    public Teacher requireAdmin(String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        if (!"ADMIN".equals(teacher.getRole())) {
            throw new BusinessException(ErrorCode.ADMIN_ACCESS_DENIED);
        }
        return teacher;
    }

    /** 현재 교사가 보유한 유효한 초대코드를 조회한다. 없으면 빈 Optional. */
    public Optional<InviteCodeResponse> getCurrentInviteCode(String authHeader) {
        Teacher teacher = getAuthenticatedTeacher(authHeader);
        return inviteCodeStore.getByTeacherId(teacher.getTeacherId())
                .map(c -> new InviteCodeResponse(c.code(), c.expiresAt()));
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
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));

        Teacher teacher = teacherMapper.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_TEACHER_NOT_FOUND));

        return new VerifyCodeResponse(true, teacher.getTeacherId(), teacher.getTeacherName());
    }

    /**
     * 학생 로그인.
     * <ol>
     *   <li>초대코드 검증 → 발급한 교사의 teacherId 획득</li>
     *   <li>이름 + 학번 + teacherId 로 student 테이블에서 학생 조회</li>
     *   <li>일치하면 JWT 발급 (subject=studentId, role=STUDENT)</li>
     * </ol>
     */
    public TokenResponse loginStudent(StudentLoginRequest request) {
        // 1) 초대코드 → teacherId
        Long teacherId = inviteCodeStore.verify(request.inviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));

        // 2) 이름 + 학번 + teacherId 로 학생 조회
        Student student = studentMapper.findByNameAndStudentNumberAndTeacherId(
                        request.studentName(), request.studentNumber(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDENT_NOT_FOUND));

        // 3) JWT 발급 — subject 를 studentId(문자열), role = STUDENT
        String token = jwtTokenProvider.createToken(
                String.valueOf(student.getStudentId()),
                "STUDENT",
                student.getStudentName());

        return TokenResponse.bearer(token);
    }
}
