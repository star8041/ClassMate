-- ============================================
-- ClassMate RDB DDL (PostgreSQL)
-- teacher/student 분리
-- chat은 교사/학생 공통 (role로 구분)
-- 상담은 schedule(schedule_type='COUNSELING')로 통합
-- ============================================

-- ============================================
-- 1. 사용자 관련
-- ============================================

-- teacher: 교사 계정 정보를 관리한다.
CREATE TABLE teacher (
                         teacher_id     BIGSERIAL PRIMARY KEY,
                         login_id       VARCHAR(50) NOT NULL UNIQUE,
                         password       VARCHAR(255) NOT NULL,
                         teacher_name   VARCHAR(50) NOT NULL,
                         email          VARCHAR(50),
                         question       VARCHAR(100) NOT NULL,
                         answer         VARCHAR(255) NOT NULL,
                         created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- student: 학생 정보를 관리한다.
CREATE TABLE student (
                         student_id     BIGSERIAL PRIMARY KEY,
                         teacher_id     BIGINT NOT NULL,
                         student_name   VARCHAR(50) NOT NULL,
                         student_number VARCHAR(50),
                         created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_student_teacher FOREIGN KEY (teacher_id)
                             REFERENCES teacher (teacher_id) ON DELETE CASCADE
);

-- ============================================
-- 2. PDF 관련
-- ============================================

-- material: 교사가 업로드한 교과서 PDF의 원본 정보를 관리한다.
CREATE TABLE material (
                          material_id   BIGSERIAL PRIMARY KEY,
                          teacher_id    BIGINT NOT NULL,
                          file_name     VARCHAR(255) NOT NULL,
                          subject       VARCHAR(50),
                          storage_path  VARCHAR(500) NOT NULL,
                          total_pages   INT NOT NULL,
                          uploaded_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_material_teacher FOREIGN KEY (teacher_id)
                              REFERENCES teacher (teacher_id) ON DELETE CASCADE
);

-- material_page: PDF에서 추출한 페이지별 원문 텍스트를 저장한다.
CREATE TABLE material_page (
                               material_page_id BIGSERIAL PRIMARY KEY,
                               material_id       BIGINT NOT NULL,
                               page_number       INT NOT NULL,
                               page_text         TEXT,
                               created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_material_page_material FOREIGN KEY (material_id)
                                   REFERENCES material (material_id) ON DELETE CASCADE,
                               CONSTRAINT uq_material_page UNIQUE (material_id, page_number)
);

-- ============================================
-- 3. 퀴즈 관련
-- ============================================

-- quiz: 퀴즈의 제목, 난이도, 출제 기준 자료 및 페이지 범위 등 전체 정보를 관리한다.
CREATE TABLE quiz (
                      quiz_id     BIGSERIAL PRIMARY KEY,
                      teacher_id  BIGINT NOT NULL,
                      material_id BIGINT,
                      title       VARCHAR(100) NOT NULL,
                      difficulty  VARCHAR(20),
                      start_page  INT,
                      end_page    INT,
                      created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      CONSTRAINT fk_quiz_teacher FOREIGN KEY (teacher_id)
                          REFERENCES teacher (teacher_id) ON DELETE CASCADE,
                      CONSTRAINT fk_quiz_material FOREIGN KEY (material_id)
                          REFERENCES material (material_id) ON DELETE SET NULL
);

-- quiz_question: 퀴즈에 포함된 문제의 내용, 선택지, 정답, 해설을 저장한다.
CREATE TABLE quiz_question (
                               quiz_question_id BIGSERIAL PRIMARY KEY,
                               quiz_id           BIGINT NOT NULL,
                               question_type     VARCHAR(20) NOT NULL,
                               question_text     TEXT NOT NULL,
                               options           JSONB,
                               answer_text       TEXT,
                               explanation       TEXT,
                               question_order    INT,
                               CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id)
                                   REFERENCES quiz (quiz_id) ON DELETE CASCADE
);
-- options 예시: ["액체→기체", "기체→액체", "고체→액체", "고체→기체"]
-- question_type: 객관식 / 주관식 / 서술형

-- quiz_attempt: 학생의 퀴즈 응시 기록(총점, 정답 수, 제출 시각)을 저장한다.
CREATE TABLE quiz_attempt (
                              quiz_attempt_id BIGSERIAL PRIMARY KEY,
                              quiz_id         BIGINT NOT NULL,
                              student_id      BIGINT NOT NULL,
                              score           INT,
                              total_count     INT,
                              correct_count   INT,
                              submitted_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id)
                                  REFERENCES quiz (quiz_id) ON DELETE CASCADE,
                              CONSTRAINT fk_attempt_student FOREIGN KEY (student_id)
                                  REFERENCES student (student_id) ON DELETE CASCADE
);

-- quiz_answer: 학생이 응시 시 문제별로 작성한 답과 정답 여부를 저장한다.
CREATE TABLE quiz_answer (
                             quiz_answer_id      BIGSERIAL PRIMARY KEY,
                             quiz_attempt_id      BIGINT NOT NULL,
                             quiz_question_id     BIGINT NOT NULL,
                             answer_text          TEXT,
                             is_correct           BOOLEAN,
                             CONSTRAINT fk_answer_attempt FOREIGN KEY (quiz_attempt_id)
                                 REFERENCES quiz_attempt (quiz_attempt_id) ON DELETE CASCADE,
                             CONSTRAINT fk_answer_question FOREIGN KEY (quiz_question_id)
                                 REFERENCES quiz_question (quiz_question_id) ON DELETE CASCADE
);
-- answer_text: 학생이 선택/작성한 답 (선택지 텍스트 또는 주관식/서술형 답)

-- ============================================
-- 4. 채팅 관련
-- ============================================

-- chat_session: 학생 또는 교사가 AI와 나눈 대화방을 관리한다.
CREATE TABLE chat_session (
                              chat_session_id BIGSERIAL PRIMARY KEY,
                              user_id           BIGINT NOT NULL,
                              role              VARCHAR(10) NOT NULL,
                              material_id       BIGINT,
                              title             VARCHAR(100),
                              created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_session_material FOREIGN KEY (material_id)
                                  REFERENCES material (material_id) ON DELETE SET NULL
);
-- user_id + role 조합으로 teacher 또는 student를 가리킴 (FK 없음, 애플리케이션에서 처리)
-- role: TEACHER / STUDENT

-- chat_message: 대화방에서 주고받은 질문과 AI 답변 메시지를 저장한다.
CREATE TABLE chat_message (
                              chat_message_id BIGSERIAL PRIMARY KEY,
                              chat_session_id   BIGINT NOT NULL,
                              role              VARCHAR(10) NOT NULL,
                              message_text      TEXT NOT NULL,
                              created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_message_session FOREIGN KEY (chat_session_id)
                                  REFERENCES chat_session (chat_session_id) ON DELETE CASCADE
);
-- role: TEACHER / STUDENT / AI

-- ============================================
-- 5. 성취도 리포트
-- ============================================

-- achievement_report: 학생의 퀴즈 결과를 분석한 AI 생성 성취도 리포트(강점, 약점, 코멘트)를 저장한다.
CREATE TABLE achievement_report (
                                    achievement_report_id BIGSERIAL PRIMARY KEY,
                                    student_id              BIGINT NOT NULL,
                                    period_start             DATE,
                                    period_end               DATE,
                                    summary_text             TEXT,
                                    strength_text            TEXT,
                                    weakness_text            TEXT,
                                    comment_text             TEXT,
                                    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_report_student FOREIGN KEY (student_id)
                                        REFERENCES student (student_id) ON DELETE CASCADE
);

-- ============================================
-- 6. 일정 관련
-- ============================================

-- schedule: 교사의 상담/수업/기타 일정을 관리한다. 상담인 경우 학생, 주제, 캘린더 연동 정보를 함께 관리한다.
CREATE TABLE schedule (
                          schedule_id        BIGSERIAL PRIMARY KEY,
                          teacher_id          BIGINT NOT NULL,
                          student_id          BIGINT,
                          schedule_type       VARCHAR(20) NOT NULL,
                          title               VARCHAR(200) NOT NULL,
                          topic               VARCHAR(200),
                          scheduled_at        TIMESTAMP NOT NULL,
                          status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                          calendar_event_id   VARCHAR(255),
                          created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_schedule_teacher FOREIGN KEY (teacher_id)
                              REFERENCES teacher (teacher_id) ON DELETE CASCADE,
                          CONSTRAINT fk_schedule_student FOREIGN KEY (student_id)
                              REFERENCES student (student_id) ON DELETE CASCADE
);
-- schedule_type: COUNSELING(상담) / CLASS(수업) / ETC(기타)
-- student_id, topic: COUNSELING인 경우에만 사용

-- counseling_note: 상담 일정에 연결된 녹음 원문, AI 요약, 후속 조치 내용을 저장한다.
CREATE TABLE counseling_note (
                                 counseling_note_id BIGSERIAL PRIMARY KEY,
                                 schedule_id          BIGINT NOT NULL,
                                 raw_text             TEXT,
                                 summary_text         TEXT,
                                 follow_up_text       TEXT,
                                 created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_note_schedule FOREIGN KEY (schedule_id)
                                     REFERENCES schedule (schedule_id) ON DELETE CASCADE
);
