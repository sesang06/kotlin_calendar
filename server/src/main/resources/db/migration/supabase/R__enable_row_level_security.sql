-- Supabase 전용 (DatabaseVendor.SUPABASE 에서만 실행).
-- Supabase는 public 스키마를 Data API(PostgREST)로 anon/authenticated 역할에 노출한다.
-- 서버는 테이블 소유자 역할로 접속하므로 RLS의 영향을 받지 않는다.
-- 정책 없이 RLS만 켜서 Data API를 통한 직접 접근을 모두 막는다.
-- 새 테이블을 만들면 여기에 한 줄 추가한다 (반복 마이그레이션이라 내용이 바뀌면 다시 실행된다).
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
