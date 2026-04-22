-- article_category enum을 FASHION, MUSIC, ART, ETC로 교체
-- 데이터 없는 상태이므로 컬럼 타입을 text로 변환 후 재생성

ALTER TABLE articles ALTER COLUMN category TYPE VARCHAR(20);
DROP TYPE article_category;
CREATE TYPE article_category AS ENUM ('FASHION', 'MUSIC', 'ART', 'ETC');
ALTER TABLE articles ALTER COLUMN category TYPE article_category USING category::article_category;
