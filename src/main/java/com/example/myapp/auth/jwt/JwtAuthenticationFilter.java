package com.example.myapp.auth.jwt;

import com.example.myapp.teacher.mapper.TeacherMapper;
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
 * 요청마다 Authorization 헤더의 Bearer JWT를 검증하고,
 * 유효하면 teacherId를 principal로 SecurityContext에 등록한다.
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
                String loginId = jwtTokenProvider.getSubject(token);
                String role    = jwtTokenProvider.parse(token).get("role", String.class);

                teacherMapper.findByLoginId(loginId).ifPresent(teacher -> {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    teacher.getTeacherId(),          // principal → @AuthenticationPrincipal Long teacherId
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

            } catch (JwtException | IllegalArgumentException ignored) {
                // 유효하지 않은 토큰 → 인증 없이 통과 (permitAll 경로는 그냥 진행)
            }
        }

        filterChain.doFilter(request, response);
    }
}
