package com.example.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/test",
                    "/archive",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    // TODO: 인증 연동 전까지 PDF 자료 API 임시 허용
                    "/api/materials/**",
                    // 인증/사용자 API (내 정보 조회는 컨트롤러에서 토큰 검증)
                    "/api/v1/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .permitAll()
            );

        return http.build();
    }

    /** 비밀번호 해시 인코더 (BCrypt) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
