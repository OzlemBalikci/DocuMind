-- InterviewPilot - initial schema
-- Kopyala: src/main/resources/db/migration/V1__init_schema.sql

CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

-- E-posta büyük/küçük harf duyarsız benzersiz
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));


CREATE TABLE interview_sessions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_title         VARCHAR(120) NOT NULL,
    seniority         VARCHAR(20)  NOT NULL,
    tech_stack        VARCHAR(500) NOT NULL,
    job_description   TEXT,
    language          VARCHAR(5)   NOT NULL DEFAULT 'TR',
    question_count    SMALLINT     NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    overall_score     NUMERIC(4, 2),
    summary_feedback  TEXT,
    llm_model         VARCHAR(100),
    error_message     VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,
    CONSTRAINT chk_sessions_seniority CHECK (seniority IN ('INTERN', 'JUNIOR', 'MID', 'SENIOR')),
    CONSTRAINT chk_sessions_language  CHECK (language IN ('TR', 'EN')),
    CONSTRAINT chk_sessions_status    CHECK (status IN ('PENDING', 'GENERATING', 'READY', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_sessions_qcount    CHECK (question_count BETWEEN 3 AND 10),
    CONSTRAINT chk_sessions_score     CHECK (overall_score IS NULL OR overall_score BETWEEN 0 AND 10)
);

CREATE INDEX ix_sessions_user_created ON interview_sessions (user_id, created_at DESC);


CREATE TABLE questions (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id           BIGINT       NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    order_index          SMALLINT     NOT NULL,
    question_text        TEXT         NOT NULL,
    category             VARCHAR(30)  NOT NULL,
    difficulty           VARCHAR(10)  NOT NULL,
    expected_key_points  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ux_questions_session_order UNIQUE (session_id, order_index),
    CONSTRAINT chk_questions_category   CHECK (category IN ('TECHNICAL', 'BEHAVIORAL', 'SYSTEM_DESIGN', 'PROBLEM_SOLVING')),
    CONSTRAINT chk_questions_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);


-- Cevap, değerlendirme gelmeden önce kaydedilir: score ve ideal_answer bu yüzden nullable.
CREATE TABLE answers (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id        BIGINT      NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    answer_text        TEXT        NOT NULL,
    evaluation_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    score              SMALLINT,
    strengths          JSONB       NOT NULL DEFAULT '[]'::jsonb,
    improvements       JSONB       NOT NULL DEFAULT '[]'::jsonb,
    ideal_answer       TEXT,
    error_message      VARCHAR(500),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    evaluated_at       TIMESTAMPTZ,
    CONSTRAINT ux_answers_question    UNIQUE (question_id),
    CONSTRAINT chk_answers_eval_status CHECK (evaluation_status IN ('PENDING', 'RUNNING', 'DONE', 'FAILED')),
    CONSTRAINT chk_answers_score      CHECK (score IS NULL OR score BETWEEN 0 AND 10),
    -- Değerlendirme bittiyse puan ve ideal cevap dolmuş olmalı
    CONSTRAINT chk_answers_done_fields CHECK (
        evaluation_status <> 'DONE' OR (score IS NOT NULL AND ideal_answer IS NOT NULL)
    )
);


-- Kuyruğa atılan her LLM işinin izi. DLQ'ya düşen işleri buradan görürsün.
CREATE TABLE llm_jobs (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id     BIGINT       NOT NULL REFERENCES interview_sessions (id) ON DELETE CASCADE,
    answer_id      BIGINT       REFERENCES answers (id) ON DELETE CASCADE,
    type           VARCHAR(30)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    attempts       SMALLINT     NOT NULL DEFAULT 0,
    error_message  VARCHAR(500),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    CONSTRAINT chk_jobs_type   CHECK (type IN ('GENERATE_QUESTIONS', 'EVALUATE_ANSWER', 'SUMMARIZE_SESSION')),
    CONSTRAINT chk_jobs_status CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
    -- EVALUATE_ANSWER işinin bir cevabı olmalı, diğerlerinin olmamalı
    CONSTRAINT chk_jobs_answer CHECK (
        (type = 'EVALUATE_ANSWER' AND answer_id IS NOT NULL)
        OR (type <> 'EVALUATE_ANSWER' AND answer_id IS NULL)
    )
);

CREATE INDEX ix_jobs_session ON llm_jobs (session_id);
-- "Takılı kalan işler" ve admin görünümü için
CREATE INDEX ix_jobs_status_created ON llm_jobs (status, created_at);


CREATE TABLE llm_usage_logs (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    session_id          BIGINT       REFERENCES interview_sessions (id) ON DELETE SET NULL,
    job_id              BIGINT       REFERENCES llm_jobs (id) ON DELETE SET NULL,
    operation           VARCHAR(30)  NOT NULL,
    model               VARCHAR(100) NOT NULL,
    prompt_tokens       INTEGER,
    completion_tokens   INTEGER,
    latency_ms          INTEGER      NOT NULL,
    success             BOOLEAN      NOT NULL,
    error_message       VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_usage_operation CHECK (operation IN ('GENERATE_QUESTIONS', 'EVALUATE_ANSWER', 'SUMMARIZE_SESSION'))
);

-- Günlük kota sorgusu: WHERE user_id = ? AND created_at >= <bugün 00:00>
CREATE INDEX ix_usage_user_created ON llm_usage_logs (user_id, created_at);
