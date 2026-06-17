package com.example.myapp.auth.jwt;

import com.example.myapp.teacher.mapper.TeacherMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 요청마다 Authorization 헤더의 Bearer JWT를 검증하고 SecurityContext에 등록한다.
 *
 * <ul>
 *   <li>교사(USER/ADMIN): subject = loginId → teacherMapper로 teacherId 조회 → principal</li>
 *   <li>학생(STUDENT): subject = studentId(문자열) → Long 변환 → principal</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TeacherMapper teacherMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parse(token);
                String subject = claims.getSubject();
                String role    = claims.get("role", String.class);

                if ("STUDENT".equals(role)) {
                    // 학생 JWT: subject = studentId
                    Long studentId = Long.parseLong(subject);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    studentId,   // principal → @AuthenticationPrincipal Long studentId
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // 교사 JWT: subject = loginId
                    teacherMapper.findByLoginId(subject).ifPresent(teacher -> {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        teacher.getTeacherId(),   // principal → @AuthenticationPrincipal Long teacherId
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }

            } catch (JwtException | IllegalArgumentException ignored) {
                // 유효하지 않은 토큰 → 인증 없이 통과 (permitAll 경로는 그냥 진행)
            }
        }

        filterChain.doFilter(request, response);
    }
}
