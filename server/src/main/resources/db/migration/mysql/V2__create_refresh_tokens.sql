-- postgresql/V2__create_refresh_tokens.sql 과 같은 스키마 (MySQL 8.0.16+).
CREATE TABLE refresh_tokens (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    token_hash CHAR(64)    NOT NULL,
    expires_at BIGINT      NOT NULL,
    created_at BIGINT      NOT NULL,
    revoked_at BIGINT,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
