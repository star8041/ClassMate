package com.example.myapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회원 관련 페이지 라우트.
 *
 * <pre>
 * GET /members/logout   로그아웃 (클라이언트 토큰 정리 후 /login 으로 이동)
 * </pre>
 *
 * 기존엔 /members/logout 라우트가 없어 404 → GlobalExceptionHandler 가 500 으로
 * 변환하던 문제를 해결한다. (JWT 는 클라이언트 보관이라 서버 세션 무효화는 불필요)
 */
@Controller
public class MemberPageController {

    @GetMapping("/members/logout")
    public String logout() {
        return "logout";
    }
}
