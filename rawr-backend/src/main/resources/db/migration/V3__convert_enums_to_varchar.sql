-- Hibernate는 PostgreSQL custom ENUM과 VARCHAR 비교 시 캐스팅 오류 발생
-- 컬럼을 VARCHAR로 변경하여 호환성 확보

ALTER TABLE articles ALTER COLUMN category TYPE VARCHAR(20);
ALTER TABLE articles ALTER COLUMN status TYPE VARCHAR(20);

DROP TYPE IF EXISTS article_category;
DROP TYPE IF EXISTS article_status;
