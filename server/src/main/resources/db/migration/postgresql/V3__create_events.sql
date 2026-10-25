-- 사용자 일정. 소유자만 볼 수 있다. 시각은 TIMESTAMP WITH TIME ZONE (server-database 스킬의 스키마 규칙).
CREATE TABLE events (
    id          VARCHAR(36)              NOT NULL,
    -- users.id. 사용자를 지우면 그 사용자의 일정도 지워진다.
    owner_id    VARCHAR(36)              NOT NULL,
    -- 1~200자. 빈 제목은 애플리케이션에서 막는다.
    title       VARCHAR(200)             NOT NULL,
    -- 0~2000자. 빈 문자열이면 설명 없음 (NULL을 쓰지 않는다).
    description VARCHAR(2000) DEFAULT '' NOT NULL,
    start_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_events_end_after_start CHECK (end_at > start_at)
);

-- "소유자의 일정 중 [from, to) 구간과 겹치는 것을 시작 순으로" 조회용.
CREATE INDEX ix_events_owner_start_end ON events (owner_id, start_at, end_at);
