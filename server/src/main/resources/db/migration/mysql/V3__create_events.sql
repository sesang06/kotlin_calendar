-- postgresql/V3__create_events.sql 과 같은 스키마 (MySQL 8.0.16+).
CREATE TABLE events (
    id          VARCHAR(36)   NOT NULL,
    owner_id    VARCHAR(36)   NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000) DEFAULT '' NOT NULL,
    start_at    DATETIME(6)   NOT NULL,
    end_at      DATETIME(6)   NOT NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_events_end_after_start CHECK (end_at > start_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_events_owner_start_end ON events (owner_id, start_at, end_at);
