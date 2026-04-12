package com.youflow.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * WebSocketController 단위 테스트.
 *
 * 테스트 전략:
 *   - RoomManager와 SimpMessagingTemplate을 Mock으로 처리
 *   - "명령을 받았을 때 올바른 RoomManager 메서드가 호출되고,
 *      올바른 채널로 브로드캐스트가 발생하는가"를 검증한다
 *   - 실제 WebSocket 연결은 검증하지 않는다 (통합 테스트의 영역)
 *
 * 커버 범위:
 *   - PLAY, PAUSE, SEEK, CHANGE_VIDEO, VOLUME 명령 처리
 *   - 올바른 /topic/room/{email} 채널로 브로드캐스트 발생 여부
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock private RoomManager roomManager;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private WebSocketController webSocketController;

    private final String EMAIL = "user@example.com";
    private final String TOPIC = "/topic/room/" + EMAIL;

    // ═══════════════════════════════════════════════
    // PLAY
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("PLAY 명령 — roomManager.play() 호출 후 상태 브로드캐스트")
    void handleCommand_play() {
        // given
        RoomState state = new RoomState();
        given(roomManager.play(EMAIL)).willReturn(state);

        // when
        PlayerCommand command = new PlayerCommand(PlayerCommand.Type.PLAY, null, null, null, null);
        webSocketController.handleCommand(EMAIL, command);

        // then: play()가 호출되고 브로드캐스트가 발생했는지 확인
        then(roomManager).should().play(EMAIL);
        then(messagingTemplate).should()
                .convertAndSend(eq(TOPIC), eq(PlayerState.from(state)));
    }

    // ═══════════════════════════════════════════════
    // PAUSE
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("PAUSE 명령 — roomManager.pause() 호출 후 상태 브로드캐스트")
    void handleCommand_pause() {
        // given
        RoomState state = new RoomState();
        given(roomManager.pause(EMAIL)).willReturn(state);

        // when
        PlayerCommand command = new PlayerCommand(PlayerCommand.Type.PAUSE, null, null, null, null);
        webSocketController.handleCommand(EMAIL, command);

        // then
        then(roomManager).should().pause(EMAIL);
        then(messagingTemplate).should()
                .convertAndSend(eq(TOPIC), eq(PlayerState.from(state)));
    }

    // ═══════════════════════════════════════════════
    // SEEK
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("SEEK 명령 — roomManager.seek() 에 positionSec 전달 후 브로드캐스트")
    void handleCommand_seek() {
        // given
        RoomState state = new RoomState();
        given(roomManager.seek(EMAIL, 42.5)).willReturn(state);

        // when
        PlayerCommand command = new PlayerCommand(PlayerCommand.Type.SEEK, 42.5, null, null, null);
        webSocketController.handleCommand(EMAIL, command);

        // then
        then(roomManager).should().seek(EMAIL, 42.5);
        then(messagingTemplate).should()
                .convertAndSend(eq(TOPIC), eq(PlayerState.from(state)));
    }

    // ═══════════════════════════════════════════════
    // CHANGE_VIDEO
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("CHANGE_VIDEO 명령 — roomManager.changeVideo() 에 videoId/youtubeId 전달 후 브로드캐스트")
    void handleCommand_changeVideo() {
        // given
        RoomState state = new RoomState();
        given(roomManager.changeVideo(EMAIL, 5L, "abc123")).willReturn(state);

        // when
        PlayerCommand command = new PlayerCommand(PlayerCommand.Type.CHANGE_VIDEO, null, 5L, "abc123", null);
        webSocketController.handleCommand(EMAIL, command);

        // then
        then(roomManager).should().changeVideo(EMAIL, 5L, "abc123");
        then(messagingTemplate).should()
                .convertAndSend(eq(TOPIC), eq(PlayerState.from(state)));
    }

    // ═══════════════════════════════════════════════
    // VOLUME
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("VOLUME 명령 — roomManager.updateVolume() 에 volume 전달 후 브로드캐스트")
    void handleCommand_volume() {
        // given
        RoomState state = new RoomState();
        given(roomManager.updateVolume(EMAIL, 85)).willReturn(state);

        // when
        PlayerCommand command = new PlayerCommand(PlayerCommand.Type.VOLUME, null, null, null, 85);
        webSocketController.handleCommand(EMAIL, command);

        // then
        then(roomManager).should().updateVolume(EMAIL, 85);
        then(messagingTemplate).should()
                .convertAndSend(eq(TOPIC), eq(PlayerState.from(state)));
    }
}
