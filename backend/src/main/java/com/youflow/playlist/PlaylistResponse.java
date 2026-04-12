package com.youflow.playlist;

import java.time.LocalDateTime;

/**
 * 플레이리스트 응답 DTO.
 *
 * Playlist 엔티티를 직접 Controller까지 올리면 JPA 세션 문제나
 * 원치 않는 연관 데이터 노출 위험이 있다.
 * 이 DTO가 클라이언트에 전달할 필드를 명확히 정의한다.
 */
public record PlaylistResponse(
        /** 플레이리스트 고유 ID */
        Long id,

        /** 플레이리스트 이름 */
        String name,

        /** 생성 시각 (ISO 8601 형식으로 직렬화됨) */
        LocalDateTime createdAt
) {

    /**
     * Playlist 엔티티 → PlaylistResponse DTO 변환.
     *
     * Service 레이어에서 엔티티를 반환하기 전에 이 메서드로 변환한다.
     * 변환 로직을 DTO 안에 두어 Service 코드를 깔끔하게 유지한다.
     *
     * @param playlist 변환할 플레이리스트 엔티티
     * @return PlaylistResponse DTO
     */
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getCreatedAt()
        );
    }
}
