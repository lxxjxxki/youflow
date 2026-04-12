import { useState } from 'react';

/**
 * YouTube URL 입력 폼 컴포넌트.
 *
 * 역할:
 *   - 유저가 YouTube URL을 입력하고 추가 버튼을 누르면 onAdd(url)을 호출한다.
 *   - 로딩 중에는 입력과 버튼을 비활성화해서 중복 제출을 방지한다.
 *   - 에러 메시지는 부모(ControllerPage)에서 내려받아 표시한다.
 *
 * @param {function} onAdd   URL 추가 요청 핸들러. Promise를 반환해야 한다.
 * @param {string}   error   표시할 에러 메시지 (없으면 null)
 */
export default function AddVideoForm({ onAdd, error }) {
  const [url, setUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!url.trim()) return;

    setIsLoading(true);
    try {
      await onAdd(url.trim());
      setUrl(''); // 성공 시 입력창 초기화
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={styles.form}>
      <input
        type="text"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        placeholder="YouTube URL 붙여넣기..."
        disabled={isLoading}
        style={styles.input}
      />
      <button type="submit" disabled={isLoading || !url.trim()} style={styles.button}>
        {isLoading ? '추가 중...' : '추가'}
      </button>
      {/* 에러 메시지: 유효하지 않은 URL, 존재하지 않는 영상 등 */}
      {error && <p style={styles.error}>{error}</p>}
    </form>
  );
}

const styles = {
  form: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '8px',
    padding: '12px 16px',
    borderBottom: '1px solid #2a2a2a',
  },
  input: {
    flex: 1,
    minWidth: '0',
    backgroundColor: '#272727',
    border: '1px solid #333',
    borderRadius: '6px',
    color: '#fff',
    fontSize: '13px',
    padding: '8px 12px',
    outline: 'none',
  },
  button: {
    backgroundColor: '#ff0000',
    border: 'none',
    borderRadius: '6px',
    color: '#fff',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: '600',
    padding: '8px 16px',
    whiteSpace: 'nowrap',
  },
  error: {
    color: '#ff4444',
    fontSize: '12px',
    margin: '0',
    width: '100%',
  },
};
