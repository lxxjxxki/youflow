import { useState } from 'react';
import { updateVolume as updateVolumeApi } from '../api/video';
import usePlaylistStore from '../store/playlistStore';

/**
 * 영상 목록 한 행 컴포넌트.
 *
 * 표시 요소:
 *   - 썸네일 이미지
 *   - 영상 제목
 *   - 볼륨 슬라이더 (0~100, 변경 시 즉시 DB 반영)
 *   - 재생 버튼 (WebSocket으로 CHANGE_VIDEO 명령 전송)
 *   - 삭제 버튼
 *
 * 볼륨 조절 UX:
 *   슬라이더를 드래그하는 동안은 로컬 상태만 변경(즉각적 피드백).
 *   onMouseUp/onTouchEnd에서 서버 API를 호출해 DB에 저장.
 *   이 방식으로 드래그 중 과도한 API 요청을 방지한다.
 *
 * @param {Object}   video        VideoResponse 객체
 * @param {boolean}  isPlaying    현재 이 영상이 재생 중인지 (Player View 강조 표시용)
 * @param {function} onPlay       재생 버튼 클릭 핸들러 (video 객체를 인수로 전달)
 * @param {function} onRemove     삭제 버튼 클릭 핸들러 (videoId를 인수로 전달)
 */
export default function VideoItem({ video, isPlaying, onPlay, onRemove }) {
  const selectedPlaylistId = usePlaylistStore((s) => s.selectedPlaylistId);
  const setVideoVolume = usePlaylistStore((s) => s.setVideoVolume);

  /** 슬라이더 드래그 중 로컬 볼륨 값 */
  const [localVolume, setLocalVolume] = useState(video.volume);

  /** 슬라이더를 놓았을 때 서버에 저장 */
  const handleVolumeCommit = async () => {
    if (localVolume === video.volume) return; // 변경 없으면 API 호출 생략
    try {
      await updateVolumeApi(selectedPlaylistId, video.id, localVolume);
      // 스토어 상태도 동기화
      setVideoVolume(video.id, localVolume);
    } catch {
      // 저장 실패 시 슬라이더를 원래 값으로 복원
      setLocalVolume(video.volume);
    }
  };

  return (
    <div style={{ ...styles.row, ...(isPlaying ? styles.rowPlaying : {}) }}>
      {/* 썸네일 */}
      <img
        src={video.thumbnail}
        alt={video.title}
        style={styles.thumbnail}
      />

      {/* 영상 정보 + 볼륨 */}
      <div style={styles.info}>
        <p style={styles.title} title={video.title}>
          {video.title}
        </p>

        {/* 볼륨 슬라이더 */}
        <div style={styles.volumeRow}>
          {/* 🔈 아이콘 (텍스트) */}
          <span style={styles.volumeIcon}>
            {localVolume === 0 ? '🔇' : localVolume < 50 ? '🔉' : '🔊'}
          </span>
          <input
            type="range"
            min={0}
            max={100}
            value={localVolume}
            onChange={(e) => setLocalVolume(Number(e.target.value))}
            onMouseUp={handleVolumeCommit}
            onTouchEnd={handleVolumeCommit}
            style={styles.slider}
          />
          <span style={styles.volumeValue}>{localVolume}</span>
        </div>
      </div>

      {/* 액션 버튼 */}
      <div style={styles.actions}>
        <button
          onClick={() => onPlay(video)}
          style={{ ...styles.actionBtn, ...(isPlaying ? styles.playingBtn : {}) }}
          title="재생"
        >
          {isPlaying ? '▶ 재생 중' : '▶'}
        </button>
        <button
          onClick={() => onRemove(video.id)}
          style={{ ...styles.actionBtn, ...styles.removeBtn }}
          title="삭제"
        >
          ✕
        </button>
      </div>
    </div>
  );
}

const styles = {
  row: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '10px 16px',
    borderBottom: '1px solid #1e1e1e',
    transition: 'background-color 0.15s',
  },
  rowPlaying: {
    backgroundColor: '#1a1a2e', // 현재 재생 중인 영상 강조
    borderLeft: '3px solid #ff0000',
  },
  thumbnail: {
    width: '80px',
    height: '45px',
    objectFit: 'cover',
    borderRadius: '4px',
    flexShrink: 0,
  },
  info: {
    flex: 1,
    minWidth: 0, // flex 내 텍스트 overflow 방지
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  title: {
    color: '#fff',
    fontSize: '13px',
    margin: 0,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
  volumeRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
  },
  volumeIcon: {
    fontSize: '12px',
    width: '16px',
    textAlign: 'center',
  },
  slider: {
    flex: 1,
    accentColor: '#ff0000', // 슬라이더 색상을 YouTube 레드로
    cursor: 'pointer',
  },
  volumeValue: {
    color: '#888',
    fontSize: '11px',
    width: '24px',
    textAlign: 'right',
  },
  actions: {
    display: 'flex',
    gap: '6px',
    flexShrink: 0,
  },
  actionBtn: {
    backgroundColor: '#272727',
    border: '1px solid #333',
    borderRadius: '6px',
    color: '#ccc',
    cursor: 'pointer',
    fontSize: '12px',
    padding: '4px 10px',
    transition: 'background-color 0.15s',
  },
  playingBtn: {
    backgroundColor: '#ff0000',
    borderColor: '#ff0000',
    color: '#fff',
  },
  removeBtn: {
    color: '#888',
  },
};
