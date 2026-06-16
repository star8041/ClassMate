package com.example.myapp.config;

import com.example.myapp.teacher.entity.Teacher;
import com.example.myapp.teacher.mapper.TeacherMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 관리자(ADMIN) 계정 자동 시더.
 * <p>
 * 앱 시작 시 관리자 계정이 없으면 생성한다. (코드로 보장하므로 팀원 누구나 앱만 켜면 동일 계정 보유)
 * 비밀번호는 BCrypt 로 해시 저장한다.
 * <p>
 * TODO: 운영 환경에서는 {@code app.admin.init-enabled=false} 로 끄고, 강력한 비밀번호로 별도 생성할 것.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final TeacherMapper teacherMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String loginId;
    private final String password;
    private final String name;

    public AdminAccountInitializer(TeacherMapper teacherMapper,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.admin.init-enabled:true}") boolean enabled,
                                   @Value("${app.admin.login-id:admin}") String loginId,
                                   @Value("${app.admin.password:admin}") String password,
                                   @Value("${app.admin.name:관리자}") String name) {
        this.teacherMapper = teacherMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (teacherMapper.existsByLoginId(loginId)) {
            log.info("관리자 계정 '{}' 이(가) 이미 존재하여 시더를 건너뜁니다.", loginId);
            return;
        }
        Teacher admin = Teacher.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .teacherName(name)
                .email(loginId + "@classmate.local")
                .question("")
                .answer("")
                .build();
        teacherMapper.insert(admin);
        log.warn("관리자 계정 '{}' 생성 완료 (ROLE_ADMIN). 개발용 기본 비밀번호이니 운영 전 변경하세요.", loginId);
    }
}
