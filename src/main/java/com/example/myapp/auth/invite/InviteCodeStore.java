package com.example.myapp.auth.invite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 교사 초대 코드 임시 저장소 (인메모리 + TTL).
 * <p>
 * DB 컬럼 없이 서버 메모리에 일정 시간만 보관한다.
 * <ul>
 *     <li>교사가 코드를 생성하면 {@code 코드 → teacherId} 매핑을 TTL 동안 저장</li>
 *     <li>학생이 코드를 제출하면 해당 매핑을 조회해 교사를 식별</li>
 * </ul>
 * <b>한계</b>: 서버 재시작 시 사라지고, 다중 인스턴스 환경에선 공유되지 않는다.
 * (운영 확장 시 Redis 등으로 교체 — TODO)
 */
@Component
public class InviteCodeStore {

    /** 발급 결과: 코드 + 만료 시각 */
    public record IssuedCode(String code, Instant expiresAt) {
    }

    private record Entry(Long teacherId, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;

    public InviteCodeStore(@Value("${app.invite-code.ttl-minutes:10}") long ttlMinutes) {
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * 교사용 6자리 랜덤 코드를 생성·저장한다.
     * 같은 교사의 기존 코드는 폐기하여 항상 1개만 유효하게 한다.
     */
    public IssuedCode generate(Long teacherId) {
        store.values().removeIf(Entry::isExpired);                 // 만료분 정리
        store.entrySet().removeIf(e -> e.getValue().teacherId().equals(teacherId)); // 기존 코드 폐기

        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (store.containsKey(code));

        Instant expiresAt = Instant.now().plus(ttl);
        store.put(code, new Entry(teacherId, expiresAt));
        return new IssuedCode(code, expiresAt);
    }

    /**
     * 코드를 검증하고 연결된 teacherId 를 반환한다.
     * 없거나 만료된 코드면 빈 Optional.
     */
    public Optional<Long> verify(String code) {
        if (code == null) {
            return Optional.empty();
        }
        Entry entry = store.get(code);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            store.remove(code);
            return Optional.empty();
        }
        return Optional.of(entry.teacherId());
    }
}
