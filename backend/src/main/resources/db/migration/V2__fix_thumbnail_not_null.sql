-- videos.thumbnail 컬럼을 NOT NULL로 변경한다.
--
-- 배경:
--   V1에서 thumbnail을 nullable로 정의했으나 Video 엔티티는 @Column(nullable = false)로 선언.
--   스키마와 엔티티 간 불일치 수정.
--   YouTubeApiClient에서 medium 썸네일 없으면 default로 폴백하므로 값이 항상 존재한다.
ALTER TABLE videos ALTER COLUMN thumbnail SET NOT NULL;
