-- 로그인 세션(기기)마다 하나. 재발급 시 기존 행은 revoked_at을 채우고 새 행을 만든다.
CREATE TABLE refresh_tokens (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    -- SHA-256 hex. 원문 토큰은 저장하지 않는다.
    token_hash CHAR(64)    NOT NULL,
    expires_at BIGINT      NOT NULL,
    created_at BIGINT      NOT NULL,
    -- NULL이면 유효. 로그아웃·재발급 시각.
    revoked_at BIGINT,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
