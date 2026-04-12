package com.youflow.room;

/**
 * 서버 → 클라이언트 플레이어 상태 브로드캐스트 DTO.
 *
 * 명령 처리 후 서버가 /topic/room/{email} 채널로 이 객체를 JSON 형태로 전송한다.
 * 룸을 구독한 모든 클라이언트(Player View, Controller View)가 수신하여 UI를 동기화한다.
 *
 * 수신 예시 (JavaScript):
 *   client.subscribe('/topic/room/user@example.com', (msg) => {
 *     const state = JSON.parse(msg.body); // PlayerState
 *     player.seekTo(state.positionSec);
 *   });
 */
public record PlayerState(
        /** 현재 재생 중인 DB Video PK (영상이 없으면 null) */
        Long currentVideoId,

        /** 현재 재생 중인 YouTube 영상 ID (iframe src에 사용) */
        String currentYoutubeId,

        /** 재생 중 여부 */
        boolean playing,

        /** 현재 재생 위치 (초 단위) */
        double positionSec,

        /** 현재 음량 (0~100) */
        int volume
) {

    /**
     * RoomState 객체로부터 PlayerState DTO를 생성한다.
     * 브로드캐스트 직전에 호출한다.
     *
     * @param state 메모리 내 룸 상태
     * @return 클라이언트에 전송할 PlayerState DTO
     */
    public static PlayerState from(RoomState state) {
        return new PlayerState(
                state.getCurrentVideoId(),
                state.getCurrentYoutubeId(),
                state.isPlaying(),
                state.getPositionSec(),
                state.getVolume()
        );
    }
}
