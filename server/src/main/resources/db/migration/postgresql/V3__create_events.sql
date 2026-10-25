-- 사용자 일정. 구간은 [start_at, end_at) 반열린 구간.
CREATE TABLE events (
    id            VARCHAR(36)              NOT NULL,
    owner_user_id VARCHAR(36)              NOT NULL,
    title         VARCHAR(200)             NOT NULL,
    description   VARCHAR(2000)            NOT NULL DEFAULT '',
    start_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_events_end_after_start CHECK (end_at > start_at)
);

-- 사용자별 기간 겹침 조회 (findOverlapping / existsOverlapping).
CREATE INDEX ix_events_owner_user_id_start_at_end_at ON events (owner_user_id, start_at, end_at);
