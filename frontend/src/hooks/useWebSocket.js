import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import usePlayerStore from '../store/playerStore';

/**
 * WebSocket(STOMP) 연결 및 플레이어 상태 동기화 훅.
 *
 * 역할:
 *   1. 컴포넌트 마운트 시 STOMP 연결 수립
 *   2. /topic/room/{email} 채널 구독
 *   3. 서버 브로드캐스트(PlayerState) 수신 → playerStore 업데이트
 *   4. 컴포넌트 언마운트 시 연결 해제 (메모리 누수 방지)
 *
 * 명령 전송:
 *   이 훅이 반환하는 sendCommand 함수로 서버에 PlayerCommand를 전송한다.
 *   Player View와 Controller View 모두 이 훅을 사용한다.
 *
 * 사용 예시:
 *   const { sendCommand, isConnected } = useWebSocket(email);
 *   sendCommand({ type: 'PLAY' });
 *   sendCommand({ type: 'SEEK', positionSec: 42.5 });
 *
 * @param {string|null} email 로그인한 유저의 이메일 (룸 ID). null이면 연결하지 않는다.
 * @returns {{ sendCommand: Function, isConnected: boolean }}
 */
const useWebSocket = (email) => {
  /**
   * STOMP Client를 ref로 관리한다.
   * state가 아닌 ref를 사용하는 이유:
   *   - Client 객체가 변경되어도 리렌더링을 일으키지 않아야 한다.
   *   - 리렌더링 사이에도 동일한 Client 인스턴스를 유지해야 한다.
   */
  const clientRef = useRef(null);
  const setPlayerState = usePlayerStore((state) => state.setPlayerState);

  /**
   * isConnected를 ref가 아닌 state로 관리한다.
   * ref는 값이 바뀌어도 리렌더링을 일으키지 않으므로,
   * 실제로 연결/해제되어도 배지가 업데이트되지 않는 버그를 방지한다.
   */
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    console.warn('[WS] useEffect 실행, email:', email);
    // email이 없으면 (로그아웃 상태) 연결하지 않는다
    if (!email) return;

    const client = new Client({
      /**
       * SockJS를 WebSocket 팩토리로 사용한다.
       * SockJS는 WebSocket이 지원되지 않는 환경에서 롱폴링 등으로 자동 폴백한다.
       */
      webSocketFactory: () => new SockJS('http://localhost:8080/stomp'),

      /** 연결 성공 시 /topic/room/{email} 채널 구독 */
      onConnect: () => {
        console.warn('[WS] onConnect 성공!');
        setIsConnected(true);
        client.subscribe(`/topic/room/${email}`, (message) => {
          try {
            const playerState = JSON.parse(message.body);
            // 서버에서 받은 PlayerState를 그대로 스토어에 반영
            setPlayerState(playerState);
          } catch (err) {
            console.error('WebSocket 메시지 파싱 실패:', err);
          }
        });
      },

      /** WebSocket 연결이 닫히면 (네트워크 끊김, 서버 재시작 등) 상태 반영 */
      onWebSocketClose: (event) => {
        console.warn('[WS] onWebSocketClose:', event?.code, event?.reason);
        setIsConnected(false);
      },

      onStompError: (frame) => {
        console.error('STOMP 에러:', frame);
      },

      onWebSocketError: (event) => {
        console.error('WebSocket 연결 오류:', event);
      },

      onDisconnect: () => {
        console.warn('STOMP 연결 해제');
      },
    });

    console.warn('[WS] client.activate() 호출');
    client.activate(); // 연결 시작
    clientRef.current = client;

    // 컴포넌트 언마운트 시 연결 해제
    return () => {
      client.deactivate();
      setIsConnected(false);
      clientRef.current = null;
    };
  }, [email, setPlayerState]); // email이 바뀌면 (재로그인 등) 재연결

  /**
   * 서버에 PlayerCommand를 전송한다.
   *
   * @param {Object} command PlayerCommand 객체
   * @param {string} command.type PLAY | PAUSE | SEEK | CHANGE_VIDEO | VOLUME
   * @param {number} [command.positionSec] SEEK 시 이동할 위치 (초)
   * @param {number} [command.videoId]     CHANGE_VIDEO 시 DB Video PK
   * @param {string} [command.youtubeId]   CHANGE_VIDEO 시 YouTube 영상 ID
   * @param {number} [command.volume]      VOLUME 시 새 음량 (0~100)
   */
  const sendCommand = (command) => {
    const client = clientRef.current;
    if (!client?.connected) {
      console.warn('WebSocket이 연결되지 않은 상태에서 명령 전송 시도:', command);
      return;
    }
    client.publish({
      destination: `/app/room/${email}/command`,
      body: JSON.stringify(command),
    });
  };

  return {
    sendCommand,
    /** 현재 WebSocket 연결 상태. useState로 관리하므로 연결/해제 시 리렌더링 발생. */
    isConnected,
  };
};

export default useWebSocket;
