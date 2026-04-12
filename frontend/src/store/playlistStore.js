import { create } from 'zustand';
import {
  getPlaylists as getPlaylistsApi,
  createPlaylist as createPlaylistApi,
  deletePlaylist as deletePlaylistApi,
} from '../api/playlist';
import { getVideos as getVideosApi, addVideo as addVideoApi, removeVideo as removeVideoApi } from '../api/video';

/**
 * 플레이리스트 & 영상 상태 스토어.
 *
 * 관리하는 상태:
 *   - playlists: 내 플레이리스트 목록
 *   - selectedPlaylistId: 현재 Controller View에서 열람 중인 플레이리스트 ID
 *   - videos: 선택된 플레이리스트의 영상 목록
 *   - isLoading: 로딩 중 여부 (UI 스피너 표시용)
 *   - error: 마지막 에러 메시지
 *
 * 플레이리스트와 영상을 같은 스토어에 둔 이유:
 *   플레이리스트 선택 시 영상 목록이 즉시 로드되는 연결된 동작이기 때문이다.
 *   별도 스토어로 분리하면 selectPlaylist 시 두 스토어를 동시에 업데이트해야 해서 복잡해진다.
 */
const usePlaylistStore = create((set, get) => ({
  playlists: [],
  selectedPlaylistId: null,
  videos: [],
  isLoading: false,
  error: null,

  // ─────────────────────────────────────────────
  // 플레이리스트 액션
  // ─────────────────────────────────────────────

  /**
   * 서버에서 플레이리스트 목록을 불러와 스토어에 저장한다.
   * 로그인 직후 또는 플레이리스트 변경 후 호출한다.
   */
  fetchPlaylists: async () => {
    set({ isLoading: true, error: null });
    try {
      const playlists = await getPlaylistsApi();
      set({ playlists, isLoading: false });
    } catch (err) {
      set({ error: err.response?.data?.message ?? '플레이리스트 불러오기 실패', isLoading: false });
    }
  },

  /**
   * 새 플레이리스트를 생성하고 목록을 갱신한다.
   *
   * @param {string} name 플레이리스트 이름
   */
  createPlaylist: async (name) => {
    const newPlaylist = await createPlaylistApi(name);
    // 서버 재조회 없이 로컬 상태에 바로 추가 (낙관적 업데이트)
    set((state) => ({
      playlists: [newPlaylist, ...state.playlists],
    }));
  },

  /**
   * 플레이리스트를 삭제한다.
   * 삭제된 플레이리스트가 현재 선택 중이었다면 선택을 해제한다.
   *
   * @param {number} playlistId 삭제할 플레이리스트 ID
   */
  deletePlaylist: async (playlistId) => {
    await deletePlaylistApi(playlistId);
    set((state) => ({
      playlists: state.playlists.filter((p) => p.id !== playlistId),
      // 삭제된 플레이리스트가 선택 중이었다면 해제
      selectedPlaylistId: state.selectedPlaylistId === playlistId ? null : state.selectedPlaylistId,
      videos: state.selectedPlaylistId === playlistId ? [] : state.videos,
    }));
  },

  /**
   * 플레이리스트를 선택하고 해당 영상 목록을 불러온다.
   *
   * @param {number} playlistId 선택할 플레이리스트 ID
   */
  selectPlaylist: async (playlistId) => {
    set({ selectedPlaylistId: playlistId, videos: [], isLoading: true, error: null });
    try {
      const videos = await getVideosApi(playlistId);
      set({ videos, isLoading: false });
    } catch (err) {
      set({ error: err.response?.data?.message ?? '영상 불러오기 실패', isLoading: false });
    }
  },

  // ─────────────────────────────────────────────
  // 영상 액션
  // ─────────────────────────────────────────────

  /**
   * 영상을 추가하고 로컬 목록에 반영한다.
   *
   * @param {string} youtubeUrl 유저가 입력한 YouTube URL
   */
  addVideo: async (youtubeUrl) => {
    const { selectedPlaylistId } = get();
    if (!selectedPlaylistId) return;

    const newVideo = await addVideoApi(selectedPlaylistId, youtubeUrl);
    set((state) => ({
      videos: [...state.videos, newVideo],
    }));
  },

  /**
   * 영상을 삭제하고 로컬 목록에서 제거한다.
   *
   * @param {number} videoId 삭제할 영상 ID
   */
  removeVideo: async (videoId) => {
    const { selectedPlaylistId } = get();
    if (!selectedPlaylistId) return;

    await removeVideoApi(selectedPlaylistId, videoId);
    set((state) => ({
      // 삭제 후 position 재정렬은 서버에서 처리하므로
      // 로컬에서는 단순히 필터링만 한다
      videos: state.videos.filter((v) => v.id !== videoId),
    }));
  },

  /**
   * 영상 음량을 로컬 스토어에서 즉시 업데이트한다.
   * REST API 호출은 VideoItem 컴포넌트에서 직접 수행하고,
   * 스토어는 UI 반영만 담당한다.
   *
   * @param {number} videoId 영상 ID
   * @param {number} volume  새 음량 (0~100)
   */
  setVideoVolume: (videoId, volume) => {
    set((state) => ({
      videos: state.videos.map((v) =>
        v.id === videoId ? { ...v, volume } : v
      ),
    }));
  },
}));

export default usePlaylistStore;
