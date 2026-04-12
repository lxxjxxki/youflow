package com.youflow.room;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket(STOMP) 메시지 핸들러.
 *
 * 클라이언트가 /app/room/{email}/command 로 PlayerCommand를 전송하면,
 * RoomManager에서 상태를 업데이트하고 /topic/room/{email} 로 PlayerState를 브로드캐스트한다.
 *
 * 인증 주의사항:
 *   현재 구현은 {email}을 경로 변수로 그대로 사용한다.
 *   악의적인 클라이언트가 다른 유저의 룸에 명령을 보낼 수 있으므로,
 *   향후 WebSocket 인증 강화 시 Principal에서 이메일을 추출하는 방식으로 개선할 것.
 */
@Controller
public class WebSocketController {

    private final RoomManager roomManager;

    /**
     * SimpMessagingTemplate: 서버에서 클라이언트로 메시지를 능동적으로 전송하는 도구.
     * @SendTo 어노테이션 대신 사용하면 동적 destination 경로 지정이 가능하다.
     */
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(RoomManager roomManager, SimpMessagingTemplate messagingTemplate) {
        this.roomManager = roomManager;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 플레이어 명령 수신 및 처리.
     *
     * 수신 경로: /app/room/{email}/command
     * 브로드캐스트: /topic/room/{email}
     *
     * 명령 타입에 따라 RoomManager의 적절한 메서드를 호출하고,
     * 변경된 상태를 룸 구독자 전체에게 브로드캐스트한다.
     *
     * @param email   룸 ID (URL 경로 변수)
     * @param command 클라이언트가 전송한 플레이어 명령
     */
    @MessageMapping("/room/{email}/command")
    public void handleCommand(
            @DestinationVariable String email,
            PlayerCommand command) {

        // 각 명령에 필요한 필드가 null이면 무시한다 (잘못된 요청 방어)
        RoomState updatedState = switch (command.type()) {
            case PLAY         -> roomManager.play(email);
            case PAUSE        -> roomManager.pause(email);
            case SEEK         -> {
                if (command.positionSec() == null) yield roomManager.getOrCreate(email);
                yield roomManager.seek(email, command.positionSec());
            }
            case CHANGE_VIDEO -> {
                if (command.videoId() == null || command.youtubeId() == null) yield roomManager.getOrCreate(email);
                yield roomManager.changeVideo(email, command.videoId(), command.youtubeId());
            }
            case VOLUME       -> {
                if (command.volume() == null) yield roomManager.getOrCreate(email);
                yield roomManager.updateVolume(email, command.volume());
            }
        };

        // 변경된 상태를 룸 전체 구독자에게 브로드캐스트
        messagingTemplate.convertAndSend(
                "/topic/room/" + email,
                PlayerState.from(updatedState)
        );
    }
}
