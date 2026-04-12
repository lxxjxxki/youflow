import { create } from 'zustand';

/**
 * 플레이어 상태 스토어.
 *
 * 관리하는 상태:
 *   WebSocket으로 서버에서 수신한 PlayerState를 그대로 반영한다.
 *   Player View와 Controller View가 이 스토어를 구독해서 UI를 동기화한다.
 *
 * 업데이트 흐름:
 *   서버 WebSocket 브로드캐스트
 *     → useWebSocket 훅이 수신
 *     → playerStore.setPlayerState() 호출
 *     → Player View / Controller View 리렌더링
 *
 * 이 스토어를 직접 변경하는 것은 useWebSocket 훅뿐이다.
 * 컴포넌트에서 직접 set을 호출하지 않는다.
 */
const usePlayerStore = create((set) => ({
  /** 현재 재생 중인 DB Video PK (null = 아무것도 선택 안 됨) */
  currentVideoId: null,

  /** 현재 재생 중인 YouTube 영상 ID (iframe src에 사용) */
  currentYoutubeId: null,

  /** 재생 중 여부 */
  playing: false,

  /** 현재 재생 위치 (초 단위) */
  positionSec: 0,

  /** 현재 음량 (0~100) */
  volume: 70,

  /**
   * WebSocket에서 수신한 PlayerState로 전체 상태를 덮어쓴다.
   * useWebSocket 훅에서만 호출한다.
   *
   * @param {Object} state 서버에서 받은 PlayerState 객체
   * @param {number|null} state.currentVideoId
   * @param {string|null} state.currentYoutubeId
   * @param {boolean} state.playing
   * @param {number} state.positionSec
   * @param {number} state.volume
   */
  setPlayerState: (state) => set(state),

  /**
   * 로그아웃 또는 연결 해제 시 플레이어 상태를 초기화한다.
   */
  reset: () => set({
    currentVideoId: null,
    currentYoutubeId: null,
    playing: false,
    positionSec: 0,
    volume: 70,
  }),
}));

export default usePlayerStore;
