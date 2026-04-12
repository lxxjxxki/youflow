import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useAuthStore from '../store/authStore';
import usePlaylistStore from '../store/playlistStore';
import usePlayerStore from '../store/playerStore';
import useWebSocket from '../hooks/useWebSocket';
import VideoItem from '../components/VideoItem';
import AddVideoForm from '../components/AddVideoForm';

/**
 * Controller View — 플레이리스트 조작 메인 화면.
 *
 * 역할:
 *   - 좌측 패널: 내 플레이리스트 목록 (선택, 생성, 삭제)
 *   - 우측 패널: 선택된 플레이리스트의 영상 목록 (추가, 삭제, 볼륨 조절, 재생)
 *   - 헤더: Player View 열기 버튼, 로그아웃
 *
 * WebSocket 연결:
 *   useWebSocket(email) 훅으로 자동 연결.
 *   sendCommand()로 PLAY/PAUSE/CHANGE_VIDEO 명령을 서버에 전송한다.
 *   서버가 브로드캐스트한 PlayerState는 playerStore에 자동 반영된다.
 */
export default function ControllerPage() {
  const navigate = useNavigate();

  // ── 스토어 구독 ────────────────────────────────
  const { email, logout } = useAuthStore();
  const {
    playlists, selectedPlaylistId, videos, isLoading,
    fetchPlaylists, createPlaylist, deletePlaylist,
    selectPlaylist, addVideo, removeVideo,
  } = usePlaylistStore();
  const { currentVideoId, playing } = usePlayerStore();

  // ── WebSocket ──────────────────────────────────
  const { sendCommand, isConnected } = useWebSocket(email);

  // ── 로컬 UI 상태 ───────────────────────────────
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [addVideoError, setAddVideoError] = useState(null);

  // 마운트 시 플레이리스트 목록 로드
  useEffect(() => {
    fetchPlaylists();
  }, [fetchPlaylists]);

  // ── 핸들러 ────────────────────────────────────

  /** 플레이리스트 생성 */
  const handleCreatePlaylist = async (e) => {
    e.preventDefault();
    if (!newPlaylistName.trim()) return;
    await createPlaylist(newPlaylistName.trim());
    setNewPlaylistName('');
  };

  /** 영상 추가 — AddVideoForm에서 호출 */
  const handleAddVideo = async (url) => {
    setAddVideoError(null);
    try {
      await addVideo(url);
    } catch (err) {
      const status = err.response?.status;
      if (status === 400) setAddVideoError('유효하지 않은 YouTube URL입니다.');
      else if (status === 404) setAddVideoError('YouTube에서 영상을 찾을 수 없습니다.');
      else setAddVideoError('영상 추가에 실패했습니다.');
      throw err; // AddVideoForm의 finally가 isLoading을 해제하도록 재throw
    }
  };

  /**
   * 영상 재생 — WebSocket으로 CHANGE_VIDEO + PLAY 명령 순차 전송.
   * Player View가 이 명령을 받아 해당 영상을 iframe에 로드하고 자동 재생한다.
   */
  const handlePlay = (video) => {
    sendCommand({ type: 'CHANGE_VIDEO', videoId: video.id, youtubeId: video.youtubeId });
    // 영상별로 저장된 볼륨을 Room 상태에 반영한다.
    // 이 명령이 없으면 Player View는 항상 기본값(70)으로 재생된다.
    sendCommand({ type: 'VOLUME', volume: video.volume });
    sendCommand({ type: 'PLAY' });
  };

  /** 재생/일시정지 토글 */
  const handleTogglePlay = () => {
    sendCommand({ type: playing ? 'PAUSE' : 'PLAY' });
  };

  /** 로그아웃 */
  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  /** Player View를 새 탭으로 열기 (서브모니터에 드래그해서 사용) */
  const handleOpenPlayer = () => {
    window.open('/player', '_blank');
  };

  return (
    <div style={styles.container}>
      {/* ── 헤더 ── */}
      <header style={styles.header}>
        <div style={styles.headerLeft}>
          <h1 style={styles.logo}>youflow</h1>
          <span style={{ ...styles.badge, backgroundColor: isConnected ? '#1a7a1a' : '#7a1a1a' }}>
            {isConnected ? '● 연결됨' : '● 연결 끊김'}
          </span>
        </div>
        <div style={styles.headerRight}>
          {currentVideoId && (
            <button onClick={handleTogglePlay} style={styles.headerBtn}>
              {playing ? '⏸ 일시정지' : '▶ 재생'}
            </button>
          )}
          <button onClick={handleOpenPlayer} style={styles.headerBtn}>
            🖥 Player 열기
          </button>
          <button onClick={handleLogout} style={{ ...styles.headerBtn, ...styles.logoutBtn }}>
            로그아웃
          </button>
        </div>
      </header>

      {/* ── 메인 레이아웃: 좌측 플레이리스트 | 우측 영상 목록 ── */}
      <main style={styles.main}>

        {/* ── 좌측: 플레이리스트 패널 ── */}
        <aside style={styles.sidebar}>
          <div style={styles.panelHeader}>
            <h2 style={styles.panelTitle}>플레이리스트</h2>
          </div>

          {/* 새 플레이리스트 생성 폼 */}
          <form onSubmit={handleCreatePlaylist} style={styles.createForm}>
            <input
              type="text"
              value={newPlaylistName}
              onChange={(e) => setNewPlaylistName(e.target.value)}
              placeholder="새 플레이리스트..."
              style={styles.createInput}
            />
            <button type="submit" disabled={!newPlaylistName.trim()} style={styles.createBtn}>
              +
            </button>
          </form>

          {/* 플레이리스트 목록 */}
          <ul style={styles.list}>
            {playlists.map((pl) => (
              <li
                key={pl.id}
                onClick={() => selectPlaylist(pl.id)}
                style={{
                  ...styles.listItem,
                  ...(selectedPlaylistId === pl.id ? styles.listItemActive : {}),
                }}
              >
                <span style={styles.listItemName}>{pl.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation(); // 리스트 아이템 클릭 이벤트 차단
                    deletePlaylist(pl.id);
                  }}
                  style={styles.deleteBtn}
                  title="삭제"
                >
                  ✕
                </button>
              </li>
            ))}
            {playlists.length === 0 && (
              <p style={styles.empty}>플레이리스트가 없습니다.</p>
            )}
          </ul>
        </aside>

        {/* ── 우측: 영상 목록 패널 ── */}
        <section style={styles.videoPanel}>
          {selectedPlaylistId ? (
            <>
              <div style={styles.panelHeader}>
                <h2 style={styles.panelTitle}>
                  {playlists.find((p) => p.id === selectedPlaylistId)?.name ?? '영상 목록'}
                </h2>
                <span style={styles.videoCount}>{videos.length}개</span>
              </div>

              {/* YouTube URL 입력 폼 */}
              <AddVideoForm onAdd={handleAddVideo} error={addVideoError} />

              {/* 영상 목록 */}
              {isLoading ? (
                <p style={styles.empty}>불러오는 중...</p>
              ) : (
                <ul style={styles.videoList}>
                  {videos.map((video) => (
                    <li key={video.id} style={{ listStyle: 'none' }}>
                      <VideoItem
                        video={video}
                        isPlaying={currentVideoId === video.id}
                        onPlay={handlePlay}
                        onRemove={removeVideo}
                      />
                    </li>
                  ))}
                  {videos.length === 0 && (
                    <p style={styles.empty}>영상이 없습니다. YouTube URL을 추가해보세요.</p>
                  )}
                </ul>
              )}
            </>
          ) : (
            <div style={styles.placeholder}>
              <p>👈 왼쪽에서 플레이리스트를 선택하세요.</p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    height: '100vh',
    backgroundColor: '#0f0f0f',
    color: '#fff',
    fontFamily: "'Segoe UI', sans-serif",
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 20px',
    height: '52px',
    backgroundColor: '#1a1a1a',
    borderBottom: '1px solid #2a2a2a',
    flexShrink: 0,
  },
  headerLeft: { display: 'flex', alignItems: 'center', gap: '12px' },
  logo: { color: '#ff0000', fontSize: '20px', fontWeight: '700', margin: 0 },
  badge: { borderRadius: '99px', color: '#fff', fontSize: '11px', padding: '2px 8px' },
  headerRight: { display: 'flex', alignItems: 'center', gap: '8px' },
  headerBtn: {
    backgroundColor: '#272727', border: '1px solid #333', borderRadius: '6px',
    color: '#ccc', cursor: 'pointer', fontSize: '13px', padding: '6px 12px',
  },
  logoutBtn: { color: '#888' },
  main: { display: 'flex', flex: 1, overflow: 'hidden' },
  sidebar: {
    width: '240px', flexShrink: 0, backgroundColor: '#141414',
    borderRight: '1px solid #2a2a2a', display: 'flex',
    flexDirection: 'column', overflow: 'hidden',
  },
  panelHeader: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '14px 16px 10px',
  },
  panelTitle: { color: '#fff', fontSize: '14px', fontWeight: '600', margin: 0 },
  videoCount: { color: '#666', fontSize: '12px' },
  createForm: { display: 'flex', gap: '6px', padding: '0 12px 12px' },
  createInput: {
    flex: 1, backgroundColor: '#272727', border: '1px solid #333',
    borderRadius: '6px', color: '#fff', fontSize: '12px',
    padding: '6px 10px', outline: 'none', minWidth: 0,
  },
  createBtn: {
    backgroundColor: '#ff0000', border: 'none', borderRadius: '6px',
    color: '#fff', cursor: 'pointer', fontSize: '16px', fontWeight: '700', padding: '0 12px',
  },
  list: { flex: 1, listStyle: 'none', margin: 0, overflowY: 'auto', padding: 0 },
  listItem: {
    alignItems: 'center', cursor: 'pointer', display: 'flex',
    justifyContent: 'space-between', padding: '10px 16px', transition: 'background-color 0.15s',
  },
  listItemActive: { backgroundColor: '#1e1e1e', borderLeft: '3px solid #ff0000' },
  listItemName: {
    color: '#ddd', fontSize: '13px', overflow: 'hidden',
    textOverflow: 'ellipsis', whiteSpace: 'nowrap',
  },
  deleteBtn: { background: 'none', border: 'none', color: '#555', cursor: 'pointer', fontSize: '12px', flexShrink: 0, padding: '0 2px' },
  videoPanel: { flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' },
  videoList: { flex: 1, listStyle: 'none', margin: 0, overflowY: 'auto', padding: 0 },
  empty: { color: '#555', fontSize: '13px', padding: '24px 16px', textAlign: 'center' },
  placeholder: { alignItems: 'center', color: '#555', display: 'flex', fontSize: '14px', height: '100%', justifyContent: 'center' },
};
