-- postgresql/V1__create_users.sql 과 같은 스키마 (MySQL 8.0.16+).
CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
