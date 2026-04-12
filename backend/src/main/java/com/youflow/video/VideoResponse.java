package com.youflow.video;

import java.time.LocalDateTime;

/**
 * 영상 응답 DTO.
 *
 * Video 엔티티를 직접 Controller까지 올리지 않고,
 * 클라이언트에 필요한 필드만 담아 반환한다.
 *
 * 프론트엔드 사용 예시:
 *   - youtubeId → YouTube iframe src에 사용
 *   - volume    → iframe onReady에서 player.setVolume(volume) 호출
 *   - position  → 플레이리스트 내 재생 순서 표시
 */
public record VideoResponse(
        /** 영상 고유 ID (DB PK) */
        Long id,

        /** YouTube 영상 ID (iframe 임베드에 사용) */
        String youtubeId,

        /** 영상 제목 */
        String title,

        /** 썸네일 이미지 URL */
        String thumbnail,

        /** 플레이리스트 내 재생 순서 */
        int position,

        /**
         * 이 영상에 저장된 음량 (0~100).
         * 프론트엔드 iframe player가 onReady 이벤트에서 이 값으로 setVolume()을 호출한다.
         */
        int volume,

        /** 영상 추가 시각 */
        LocalDateTime createdAt
) {

    /**
     * Video 엔티티 → VideoResponse DTO 변환.
     *
     * @param video 변환할 영상 엔티티
     * @return VideoResponse DTO
     */
    public static VideoResponse from(Video video) {
        return new VideoResponse(
                video.getId(),
                video.getYoutubeId(),
                video.getTitle(),
                video.getThumbnail(),
                video.getPosition(),
                video.getVolume(),
                video.getCreatedAt()
        );
    }
}
