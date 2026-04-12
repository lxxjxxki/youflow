package com.youflow.room;

/**
 * 메모리 내 룸(Room) 상태 객체.
 *
 * 한 유저 계정 = 하나의 룸.
 * RoomManager가 email → RoomState로 관리한다.
 *
 * 이 객체는 DB에 저장되지 않는다. 서버 재시작 시 초기 상태로 리셋된다.
 * 영속적으로 보존해야 할 데이터(volume)는 Video 엔티티에 저장된다.
 *
 * 스레드 안전성:
 *   여러 클라이언트가 동시에 같은 룸의 상태를 변경할 수 있으므로,
 *   RoomManager에서 synchronized 블록으로 접근을 직렬화한다.
 */
public class RoomState {

    /** 현재 재생 중인 DB Video PK (초기값 null — 아무 영상도 선택되지 않은 상태) */
    private Long currentVideoId;

    /** 현재 재생 중인 YouTube 영상 ID */
    private String currentYoutubeId;

    /** 재생 중 여부 (기본값: 일시정지) */
    private boolean playing = false;

    /** 현재 재생 위치 (초 단위, 기본값: 처음) */
    private double positionSec = 0.0;

    /** 현재 음량 (0~100, 기본값: 70) */
    private int volume = 70;

    /** 빈 룸 생성 — RoomManager.getOrCreate()에서 사용 */
    public RoomState() {}

    // ─────────────────────────────────────────────
    // 상태 변경 메서드 — WebSocketController에서 호출
    // ─────────────────────────────────────────────

    /** 재생 시작 */
    public void play() {
        this.playing = true;
    }

    /** 일시정지 */
    public void pause() {
        this.playing = false;
    }

    /**
     * 재생 위치 이동.
     *
     * @param positionSec 이동할 위치 (초 단위)
     */
    public void seek(double positionSec) {
        this.positionSec = positionSec;
    }

    /**
     * 재생 중인 영상 변경.
     * 영상이 바뀌면 재생 위치를 처음으로 초기화한다.
     *
     * @param videoId    DB Video PK
     * @param youtubeId  YouTube 영상 ID
     */
    public void changeVideo(Long videoId, String youtubeId) {
        this.currentVideoId = videoId;
        this.currentYoutubeId = youtubeId;
        this.positionSec = 0.0; // 새 영상은 처음부터 시작
    }

    /**
     * 음량 변경.
     *
     * @param volume 새 음량 (0~100)
     */
    public void updateVolume(int volume) {
        this.volume = volume;
    }

    // ─────────────────────────────────────────────
    // Getter
    // ─────────────────────────────────────────────

    public Long getCurrentVideoId() { return currentVideoId; }
    public String getCurrentYoutubeId() { return currentYoutubeId; }
    public boolean isPlaying() { return playing; }
    public double getPositionSec() { return positionSec; }
    public int getVolume() { return volume; }
}
