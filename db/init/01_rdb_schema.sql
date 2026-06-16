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

-- quiz: 퀴즈의 제목, 난이도, 출제 기준 자료, 페이지 범위, 응시 가능 시간을 관리한다.
CREATE TABLE quiz (
quiz_id          BIGSERIAL PRIMARY KEY,
teacher_id       BIGINT NOT NULL,
material_id      BIGINT,
title            VARCHAR(100) NOT NULL,
difficulty       VARCHAR(20),
start_page       INT,
end_page         INT,
available_from   TIMESTAMP,
available_until  TIMESTAMP,
created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
-- options 예시:
-- [
--   {"number": 1, "text": "선입선출 구조"},
--   {"number": 2, "text": "후입선출 구조"},
--   {"number": 3, "text": "무작위 접근 구조"},
--   {"number": 4, "text": "정렬 구조"}
-- ]
-- question_type 예시: MULTIPLE_CHOICE / SHORT_ANSWER / ESSAY

-- quiz_attempt: 학생의 퀴즈 응시 기록(시작 시각, 제출 시각, 점수, 정답 수)을 저장한다.
CREATE TABLE quiz_attempt (
quiz_attempt_id BIGSERIAL PRIMARY KEY,
quiz_id         BIGINT NOT NULL,
student_id      BIGINT NOT NULL,
score           INT DEFAULT 0,
total_count     INT DEFAULT 0,
correct_count   INT DEFAULT 0,
started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
submitted_at    TIMESTAMP,
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
-- answer_text: 학생이 선택/작성한 답
-- is_correct: 제출 시 서버에서 정답과 비교해 저장한 정답 여부

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

-- ============================================
-- 7. 초기 데이터
-- ============================================

-- 테스트용 교사 1명
INSERT INTO teacher (
login_id,
password,
teacher_name,
email,
question,
answer
)
VALUES (
'teacher1',
'1234',
'테스트교사',
'teacher1@classmate.com',
'가장 좋아하는 색은?',
'파랑'
)
ON CONFLICT (login_id) DO NOTHING;

-- 테스트용 학생 18명
INSERT INTO student (
teacher_id,
student_name,
student_number
)
SELECT
t.teacher_id,
s.student_name,
s.student_number
FROM teacher t
JOIN (
VALUES
('김건', 'S001'),
('김동환', 'S002'),
('김명서', 'S003'),
('김민석', 'S004'),
('성호준', 'S005'),
('안민호', 'S006'),
('안희원', 'S007'),
('윤여옥', 'S008'),
('임석준', 'S009'),
('장준익', 'S010'),
('전찬혁', 'S011'),
('정준환', 'S012'),
('정현두', 'S013'),
('정효준', 'S014'),
('조용진', 'S015'),
('주희정', 'S016'),
('차규진', 'S017'),
('홍승민', 'S018')
) AS s(student_name, student_number)
ON TRUE
WHERE t.login_id = 'teacher1';

-- 테스트용 퀴즈 1: 초등 국어 퀴즈
WITH target_teacher AS (
SELECT teacher_id
FROM teacher
WHERE login_id = 'teacher1'
),
created_quiz AS (
INSERT INTO quiz (
teacher_id,
material_id,
title,
difficulty,
start_page,
end_page,
available_from,
available_until
)
SELECT
teacher_id,
NULL,
'초등 국어 퀴즈',
'쉬움',
1,
3,
CURRENT_TIMESTAMP - INTERVAL '1 hour',
CURRENT_TIMESTAMP + INTERVAL '7 days'
FROM target_teacher
RETURNING quiz_id
)
INSERT INTO quiz_question (
quiz_id,
question_type,
question_text,
options,
answer_text,
explanation,
question_order
)
SELECT
cq.quiz_id,
q.question_type,
q.question_text,
q.option_json::jsonb,
q.answer_text,
q.explanation,
q.question_order
FROM created_quiz cq
JOIN (
VALUES
(
'MULTIPLE_CHOICE',
'다음 중 인사말로 알맞은 것은?',
'[{"number":1,"text":"안녕하세요"},{"number":2,"text":"연필"},{"number":3,"text":"바나나"},{"number":4,"text":"자동차"}]',
'1',
'안녕하세요는 사람을 만났을 때 쓰는 인사말입니다.',
1
),
(
'MULTIPLE_CHOICE',
'다음 중 문장의 끝에 쓰는 표시는 무엇인가요?',
'[{"number":1,"text":"."},{"number":2,"text":"ㄱ"},{"number":3,"text":"사과"},{"number":4,"text":"하늘"}]',
'1',
'문장이 끝날 때에는 마침표를 사용할 수 있습니다.',
2
),
(
'MULTIPLE_CHOICE',
'다음 중 동물 이름은 무엇인가요?',
'[{"number":1,"text":"책상"},{"number":2,"text":"강아지"},{"number":3,"text":"연필"},{"number":4,"text":"가방"}]',
'2',
'강아지는 동물입니다.',
3
)
) AS q(question_type, question_text, option_json, answer_text, explanation, question_order)
ON TRUE;

-- 테스트용 퀴즈 2: 초등 수학 퀴즈
WITH target_teacher AS (
SELECT teacher_id
FROM teacher
WHERE login_id = 'teacher1'
),
created_quiz AS (
INSERT INTO quiz (
teacher_id,
material_id,
title,
difficulty,
start_page,
end_page,
available_from,
available_until
)
SELECT
teacher_id,
NULL,
'초등 수학 퀴즈',
'쉬움',
4,
6,
CURRENT_TIMESTAMP - INTERVAL '1 hour',
CURRENT_TIMESTAMP + INTERVAL '7 days'
FROM target_teacher
RETURNING quiz_id
)
INSERT INTO quiz_question (
quiz_id,
question_type,
question_text,
options,
answer_text,
explanation,
question_order
)
SELECT
cq.quiz_id,
q.question_type,
q.question_text,
q.option_json::jsonb,
q.answer_text,
q.explanation,
q.question_order
FROM created_quiz cq
JOIN (
VALUES
(
'MULTIPLE_CHOICE',
'2 + 3은 얼마인가요?',
'[{"number":1,"text":"4"},{"number":2,"text":"5"},{"number":3,"text":"6"},{"number":4,"text":"7"}]',
'2',
'2에 3을 더하면 5입니다.',
1
),
(
'MULTIPLE_CHOICE',
'10에서 4를 빼면 얼마인가요?',
'[{"number":1,"text":"5"},{"number":2,"text":"6"},{"number":3,"text":"7"},{"number":4,"text":"8"}]',
'2',
'10에서 4를 빼면 6입니다.',
2
),
(
'MULTIPLE_CHOICE',
'다음 중 가장 큰 수는 무엇인가요?',
'[{"number":1,"text":"3"},{"number":2,"text":"8"},{"number":3,"text":"5"},{"number":4,"text":"1"}]',
'2',
'3, 8, 5, 1 중 가장 큰 수는 8입니다.',
3
)
) AS q(question_type, question_text, option_json, answer_text, explanation, question_order)
ON TRUE;

-- 테스트용 퀴즈 3: 초등 과학 퀴즈
WITH target_teacher AS (
SELECT teacher_id
FROM teacher
WHERE login_id = 'teacher1'
),
created_quiz AS (
INSERT INTO quiz (
teacher_id,
material_id,
title,
difficulty,
start_page,
end_page,
available_from,
available_until
)
SELECT
teacher_id,
NULL,
'초등 과학 퀴즈',
'쉬움',
7,
9,
CURRENT_TIMESTAMP - INTERVAL '1 hour',
CURRENT_TIMESTAMP + INTERVAL '7 days'
FROM target_teacher
RETURNING quiz_id
)
INSERT INTO quiz_question (
quiz_id,
question_type,
question_text,
options,
answer_text,
explanation,
question_order
)
SELECT
cq.quiz_id,
q.question_type,
q.question_text,
q.option_json::jsonb,
q.answer_text,
q.explanation,
q.question_order
FROM created_quiz cq
JOIN (
VALUES
(
'MULTIPLE_CHOICE',
'식물이 자라는 데 필요한 것은 무엇인가요?',
'[{"number":1,"text":"물"},{"number":2,"text":"돌"},{"number":3,"text":"플라스틱"},{"number":4,"text":"유리"}]',
'1',
'식물이 자라려면 물, 햇빛, 공기 등이 필요합니다.',
1
),
(
'MULTIPLE_CHOICE',
'낮에 하늘에서 밝게 빛나는 것은 무엇인가요?',
'[{"number":1,"text":"달"},{"number":2,"text":"태양"},{"number":3,"text":"구름"},{"number":4,"text":"비"}]',
'2',
'태양은 낮에 하늘을 밝게 비추는 별입니다.',
2
),
(
'MULTIPLE_CHOICE',
'물이 얼면 무엇이 되나요?',
'[{"number":1,"text":"얼음"},{"number":2,"text":"모래"},{"number":3,"text":"바람"},{"number":4,"text":"흙"}]',
'1',
'물이 차가워져 얼면 얼음이 됩니다.',
3
)
) AS q(question_type, question_text, option_json, answer_text, explanation, question_order)
ON TRUE;

