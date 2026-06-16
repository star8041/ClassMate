package com.example.myapp.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성/검증 컴포넌트 (HS256).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** subject(로그인 ID)와 role, 이름 클레임을 담은 액세스 토큰을 발급한다. */
    public String createToken(String subject, String role, String name) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("name", name)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** 토큰을 검증하고 클레임을 반환한다. 유효하지 않으면 JwtException 계열 예외 발생. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 토큰에서 subject(로그인 ID) 추출 */
    public String getSubject(String token) {
        return parse(token).getSubject();
    }
}
