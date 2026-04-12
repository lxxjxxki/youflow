package com.youflow.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * RoomManager 단위 테스트.
 *
 * RoomManager는 외부 의존성이 없는 순수 Java 컴포넌트이므로
 * Spring Context나 Mockito 없이 직접 테스트한다.
 *
 * 커버 범위:
 *   - getOrCreate: 새 룸 생성, 동일 이메일 재조회 시 같은 객체 반환
 *   - play/pause: 재생 상태 전환
 *   - seek: 재생 위치 업데이트
 *   - changeVideo: 영상 전환 및 position 초기화
 *   - updateVolume: 음량 업데이트
 */
class RoomManagerTest {

    private RoomManager roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
    }

    // ═══════════════════════════════════════════════
    // getOrCreate
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("룸 생성 — 처음 조회 시 새 룸 생성 (기본 상태)")
    void getOrCreate_newRoom() {
        RoomState state = roomManager.getOrCreate("user@example.com");

        assertThat(state).isNotNull();
        assertThat(state.isPlaying()).isFalse();         // 기본: 일시정지
        assertThat(state.getPositionSec()).isEqualTo(0.0); // 기본: 처음
        assertThat(state.getVolume()).isEqualTo(70);      // 기본 음량
        assertThat(state.getCurrentVideoId()).isNull();   // 기본: 영상 없음
    }

    @Test
    @DisplayName("룸 조회 — 같은 이메일로 두 번 조회 시 동일 객체 반환")
    void getOrCreate_sameEmail_returnsSameInstance() {
        RoomState first = roomManager.getOrCreate("user@example.com");
        RoomState second = roomManager.getOrCreate("user@example.com");

        // 같은 객체(참조 동일성)여야 한다
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("룸 격리 — 다른 이메일은 독립적인 룸을 가진다")
    void getOrCreate_differentEmails_separateRooms() {
        RoomState roomA = roomManager.getOrCreate("user-a@example.com");
        RoomState roomB = roomManager.getOrCreate("user-b@example.com");

        // 서로 다른 객체
        assertThat(roomA).isNotSameAs(roomB);
    }

    // ═══════════════════════════════════════════════
    // play / pause
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("재생 — play() 호출 후 isPlaying = true")
    void play_setsPlayingTrue() {
        RoomState state = roomManager.play("user@example.com");

        assertThat(state.isPlaying()).isTrue();
    }

    @Test
    @DisplayName("일시정지 — pause() 호출 후 isPlaying = false")
    void pause_setsPlayingFalse() {
        roomManager.play("user@example.com");  // 먼저 재생 상태로 변경
        RoomState state = roomManager.pause("user@example.com");

        assertThat(state.isPlaying()).isFalse();
    }

    // ═══════════════════════════════════════════════
    // seek
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("위치 이동 — seek() 호출 후 positionSec 업데이트")
    void seek_updatesPosition() {
        RoomState state = roomManager.seek("user@example.com", 42.5);

        assertThat(state.getPositionSec()).isEqualTo(42.5);
    }

    // ═══════════════════════════════════════════════
    // changeVideo
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 전환 — videoId/youtubeId 업데이트 및 positionSec 초기화")
    void changeVideo_updatesVideoAndResetsPosition() {
        // given: 먼저 어느 정도 재생 진행
        roomManager.seek("user@example.com", 99.0);

        // when: 영상 전환
        RoomState state = roomManager.changeVideo("user@example.com", 5L, "newVideoId");

        // then
        assertThat(state.getCurrentVideoId()).isEqualTo(5L);
        assertThat(state.getCurrentYoutubeId()).isEqualTo("newVideoId");
        // 새 영상이므로 처음부터 시작해야 한다
        assertThat(state.getPositionSec()).isEqualTo(0.0);
    }

    // ═══════════════════════════════════════════════
    // updateVolume
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("음량 업데이트 — volume 값 반영")
    void updateVolume_setsVolume() {
        RoomState state = roomManager.updateVolume("user@example.com", 85);

        assertThat(state.getVolume()).isEqualTo(85);
    }
}
