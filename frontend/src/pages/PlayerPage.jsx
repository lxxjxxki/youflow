import { useEffect, useRef, useState } from 'react';
import useAuthStore from '../store/authStore';
import usePlayerStore from '../store/playerStore';
import useWebSocket from '../hooks/useWebSocket';

/**
 * Player View — 서브모니터 풀스크린 YouTube 플레이어.
 *
 * 사용 방법:
 *   Controller View의 "Player 열기" 버튼으로 새 탭을 열고,
 *   해당 탭을 서브모니터로 드래그 후 풀스크린(F11)으로 사용한다.
 *
 * YouTube iframe API 연동 흐름:
 *   1. 컴포넌트 마운트 시 YouTube iframe API 스크립트를 동적으로 삽입한다.
 *   2. window.onYouTubeIframeAPIReady 콜백이 호출되면 YT.Player 인스턴스를 생성한다.
 *   3. playerStore의 currentYoutubeId가 바뀌면 loadVideoById()로 영상을 교체한다.
 *   4. playing 상태가 바뀌면 playVideo() / pauseVideo()를 호출한다.
 *   5. positionSec이 바뀌면 seekTo()를 호출한다.
 *   6. onReady 이벤트에서 playerStore의 volume으로 setVolume()을 호출한다.
 *      → 영상별 저장 볼륨이 자동 적용된다.
 *
 * WebSocket:
 *   같은 useWebSocket(email) 훅으로 Controller View와 같은 룸을 구독한다.
 *   PlayerState 수신 → playerStore 업데이트 → useEffect가 iframe API 호출.
 */
export default function PlayerPage() {
  const email = useAuthStore((s) => s.email);

  // playerStore에서 필요한 상태만 구독
  const currentYoutubeId = usePlayerStore((s) => s.currentYoutubeId);
  const playing = usePlayerStore((s) => s.playing);
  const positionSec = usePlayerStore((s) => s.positionSec);
  const volume = usePlayerStore((s) => s.volume);

  // WebSocket 연결 (구독만. Player View에서 명령을 보낼 일은 없지만 훅 재사용)
  const { isConnected } = useWebSocket(email);

  /**
   * YT.Player 인스턴스를 ref로 관리한다.
   * state가 아닌 이유: 플레이어 객체 변경 시 리렌더링이 필요 없다.
   */
  const playerRef = useRef(null);

  /** YouTube API 로드 완료 여부 */
  const [apiReady, setApiReady] = useState(false);

  /** iframe이 마운트될 DOM 노드의 id */
  const PLAYER_DOM_ID = 'yt-player';

  // ── YouTube iframe API 스크립트 로드 ──────────
  useEffect(() => {
    // 이미 로드된 경우 (컴포넌트 재마운트 등) 바로 ready 처리
    if (window.YT && window.YT.Player) {
      setApiReady(true);
      return;
    }

    // window.onYouTubeIframeAPIReady는 API 스크립트가 로드 완료되면 자동 호출된다.
    window.onYouTubeIframeAPIReady = () => setApiReady(true);

    const script = document.createElement('script');
    script.src = 'https://www.youtube.com/iframe_api';
    script.async = true;
    document.body.appendChild(script);

    return () => {
      // 언마운트 시 콜백 정리 (다른 페이지에서 중복 호출 방지)
      window.onYouTubeIframeAPIReady = null;
    };
  }, []);

  // ── YT.Player 인스턴스 생성 ───────────────────
  useEffect(() => {
    if (!apiReady) return;

    playerRef.current = new window.YT.Player(PLAYER_DOM_ID, {
      height: '100%',
      width: '100%',
      playerVars: {
        autoplay: 0,
        controls: 1,
        rel: 0,            // 관련 영상 비표시
        modestbranding: 1, // YouTube 로고 최소화
        iv_load_policy: 3, // 자막/어노테이션 숨김
      },
      events: {
        onReady: (event) => {
          // 플레이어가 준비되면 현재 저장된 볼륨 적용
          event.target.setVolume(volume);
          // 이미 재생 중인 영상이 있으면 바로 로드
          if (currentYoutubeId) {
            event.target.loadVideoById(currentYoutubeId);
          }
        },
        onError: (event) => {
          console.error('YouTube Player 에러 코드:', event.data);
        },
      },
    });

    return () => {
      // 언마운트 시 플레이어 정리
      if (playerRef.current) {
        playerRef.current.destroy();
        playerRef.current = null;
      }
    };
    // apiReady가 true로 바뀔 때 한 번만 실행
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiReady]);

  // ── 영상 전환 ────────────────────────────────
  useEffect(() => {
    if (!playerRef.current || !currentYoutubeId) return;
    // loadVideoById: 새 영상을 로드한다. playing 상태는 별도 effect에서 처리.
    playerRef.current.loadVideoById(currentYoutubeId);
  }, [currentYoutubeId]);

  // ── 재생 / 일시정지 ──────────────────────────
  useEffect(() => {
    if (!playerRef.current) return;
    if (playing) {
      playerRef.current.playVideo();
    } else {
      playerRef.current.pauseVideo();
    }
  }, [playing]);

  // ── 탐색 (Seek) ──────────────────────────────
  useEffect(() => {
    if (!playerRef.current || positionSec === 0) return;
    // allowSeekAhead=true: 아직 버퍼링 안 된 구간도 바로 이동
    playerRef.current.seekTo(positionSec, true);
  }, [positionSec]);

  // ── 볼륨 ────────────────────────────────────
  useEffect(() => {
    if (!playerRef.current) return;
    playerRef.current.setVolume(volume);
  }, [volume]);

  return (
    <div style={styles.container}>
      {/* YouTube iframe이 마운트될 div */}
      <div id={PLAYER_DOM_ID} style={styles.player} />

      {/* 영상이 없을 때 대기 화면 */}
      {!currentYoutubeId && (
        <div style={styles.waiting}>
          <p style={styles.waitingLogo}>youflow</p>
          <p style={styles.waitingText}>Controller에서 영상을 선택하면 여기서 재생됩니다.</p>
          <span style={{ ...styles.badge, backgroundColor: isConnected ? '#1a7a1a' : '#7a1a1a' }}>
            {isConnected ? '● 연결됨' : '● 연결 끊김'}
          </span>
        </div>
      )}

      {/* 우측 하단 연결 상태 배지 (영상 재생 중에도 표시) */}
      {currentYoutubeId && (
        <div style={styles.statusBadge}>
          <span style={{ ...styles.badge, backgroundColor: isConnected ? '#1a7a1a' : '#7a1a1a' }}>
            {isConnected ? '● 연결됨' : '● 연결 끊김'}
          </span>
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    backgroundColor: '#000',
    height: '100vh',
    left: 0,
    overflow: 'hidden',
    position: 'fixed',
    top: 0,
    width: '100vw',
  },
  player: {
    height: '100%',
    left: 0,
    position: 'absolute',
    top: 0,
    width: '100%',
  },
  // 영상 없는 대기 상태 오버레이
  waiting: {
    alignItems: 'center',
    backgroundColor: '#0f0f0f',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    height: '100%',
    justifyContent: 'center',
    left: 0,
    position: 'absolute',
    top: 0,
    width: '100%',
  },
  waitingLogo: {
    color: '#ff0000',
    fontSize: '48px',
    fontFamily: "'Segoe UI', sans-serif",
    fontWeight: '700',
    margin: 0,
  },
  waitingText: {
    color: '#555',
    fontSize: '16px',
    fontFamily: "'Segoe UI', sans-serif",
    margin: 0,
  },
  // 영상 재생 중 우측 하단 상태 배지
  statusBadge: {
    bottom: '16px',
    position: 'absolute',
    right: '16px',
  },
  badge: {
    borderRadius: '99px',
    color: '#fff',
    fontSize: '11px',
    fontFamily: "'Segoe UI', sans-serif",
    padding: '3px 10px',
  },
};
