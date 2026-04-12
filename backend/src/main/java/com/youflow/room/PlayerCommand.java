package com.youflow.room;

/**
 * 클라이언트 → 서버 플레이어 명령 DTO.
 *
 * 클라이언트가 /app/room/{email}/command 로 이 객체를 JSON 형태로 전송한다.
 *
 * 명령 타입별 payload 사용 필드:
 *
 *   PLAY         — payload 없음 (null 허용)
 *   PAUSE        — payload 없음 (null 허용)
 *   SEEK         — positionSec: 이동할 재생 위치 (초)
 *   CHANGE_VIDEO — videoId: DB의 Video PK, youtubeId: iframe 임베드용 ID
 *   VOLUME       — volume: 새 음량 (0~100)
 *
 * 전송 예시 (JavaScript):
 *   client.publish({
 *     destination: '/app/room/user@example.com/command',
 *     body: JSON.stringify({ type: 'SEEK', positionSec: 42.5 })
 *   });
 */
public record PlayerCommand(
        /** 명령 종류 */
        Type type,

        /** SEEK 시 이동할 재생 위치 (초 단위, null 허용) */
        Double positionSec,

        /** CHANGE_VIDEO 시 DB Video PK (null 허용) */
        Long videoId,

        /** CHANGE_VIDEO 시 YouTube 영상 ID (iframe src에 사용, null 허용) */
        String youtubeId,

        /** VOLUME 시 새 음량 0~100 (null 허용) */
        Integer volume
) {

    /** 플레이어 명령 종류 */
    public enum Type {
        /** 재생 시작 */
        PLAY,

        /** 일시정지 */
        PAUSE,

        /** 특정 위치로 이동 */
        SEEK,

        /** 다른 영상으로 전환 */
        CHANGE_VIDEO,

        /** 음량 변경 */
        VOLUME
    }
}
