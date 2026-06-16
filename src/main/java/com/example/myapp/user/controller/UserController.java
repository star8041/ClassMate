package com.example.myapp.user.controller;

import com.example.myapp.auth.jwt.JwtTokenProvider;
import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import com.example.myapp.user.dto.MeResponse;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자(내 정보) API.
 *
 * <pre>
 * GET /api/v1/users/me   내 정보 조회 (Authorization: Bearer 토큰 필요)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final JwtTokenProvider jwtTokenProvider;
    private final TeacherMapper teacherMapper;

    public UserController(JwtTokenProvider jwtTokenProvider, TeacherMapper teacherMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.teacherMapper = teacherMapper;
    }

    /** 내 정보 조회: Bearer 토큰의 subject(로그인 ID)로 교사 정보를 반환한다. */
    @GetMapping("/me")
    public MeResponse me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다.");
        }
        String token = authHeader.substring("Bearer ".length());

        String loginId;
        try {
            loginId = jwtTokenProvider.getSubject(token);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", e);
        }

        Teacher teacher = teacherMapper.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 사용자입니다."));
        return MeResponse.ofTeacher(teacher);
    }
}
