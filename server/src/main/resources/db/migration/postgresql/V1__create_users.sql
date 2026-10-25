-- 가입 사용자. 시각은 UTC epoch millis (server-database 스킬의 스키마 규칙).
CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL,
    -- 애플리케이션이 소문자로 정규화해서 저장한다.
    email         VARCHAR(320) NOT NULL,
    -- bcrypt 등 단방향 해시. 원문 비밀번호는 저장하지 않는다.
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email))
);
