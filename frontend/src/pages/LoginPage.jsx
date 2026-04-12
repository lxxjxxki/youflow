import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useAuthStore from '../store/authStore';

/**
 * 로그인 페이지.
 *
 * 흐름:
 *   1. 이메일/비밀번호 입력
 *   2. authStore.login() 호출 → 서버 POST /api/auth/login
 *   3. 성공: JWT 저장 후 /controller로 이동
 *   4. 실패: 서버 에러 메시지를 폼 하단에 표시
 */
export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(email, password);
      navigate('/controller');
    } catch (err) {
      // 서버 응답 메시지 표시, 없으면 기본 메시지
      const status = err.response?.status;
      if (status === 401) {
        setError('이메일 또는 비밀번호가 올바르지 않습니다.');
      } else {
        setError('로그인 중 오류가 발생했습니다. 다시 시도해주세요.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* 로고 / 타이틀 */}
        <h1 style={styles.title}>youflow</h1>
        <p style={styles.subtitle}>YouTube 플레이리스트 매니저</p>

        <form onSubmit={handleSubmit} style={styles.form}>
          {/* 이메일 */}
          <div style={styles.field}>
            <label style={styles.label}>이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              style={styles.input}
            />
          </div>

          {/* 비밀번호 */}
          <div style={styles.field}>
            <label style={styles.label}>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              style={styles.input}
            />
          </div>

          {/* 에러 메시지 */}
          {error && <p style={styles.error}>{error}</p>}

          {/* 로그인 버튼 */}
          <button
            type="submit"
            disabled={isLoading}
            style={{ ...styles.button, opacity: isLoading ? 0.7 : 1 }}
          >
            {isLoading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        {/* 회원가입 링크 */}
        <p style={styles.footer}>
          계정이 없으신가요?{' '}
          <Link to="/register" style={styles.link}>
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}

/** 인라인 스타일 — 별도 CSS 파일 없이 컴포넌트 단위로 관리 */
const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#0f0f0f', // YouTube 스타일 다크 배경
    fontFamily: "'Segoe UI', sans-serif",
  },
  card: {
    backgroundColor: '#1a1a1a',
    borderRadius: '12px',
    padding: '40px',
    width: '100%',
    maxWidth: '400px',
    boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
  },
  title: {
    color: '#ff0000', // YouTube 레드
    fontSize: '28px',
    fontWeight: '700',
    margin: '0 0 4px',
    letterSpacing: '-0.5px',
  },
  subtitle: {
    color: '#aaa',
    fontSize: '13px',
    margin: '0 0 32px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  field: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
  },
  label: {
    color: '#ccc',
    fontSize: '13px',
    fontWeight: '500',
  },
  input: {
    backgroundColor: '#272727',
    border: '1px solid #333',
    borderRadius: '8px',
    color: '#fff',
    fontSize: '14px',
    padding: '10px 14px',
    outline: 'none',
    transition: 'border-color 0.2s',
  },
  error: {
    color: '#ff4444',
    fontSize: '13px',
    margin: '0',
    padding: '8px 12px',
    backgroundColor: 'rgba(255,68,68,0.1)',
    borderRadius: '6px',
    border: '1px solid rgba(255,68,68,0.2)',
  },
  button: {
    backgroundColor: '#ff0000',
    border: 'none',
    borderRadius: '8px',
    color: '#fff',
    cursor: 'pointer',
    fontSize: '15px',
    fontWeight: '600',
    marginTop: '4px',
    padding: '12px',
    transition: 'background-color 0.2s',
  },
  footer: {
    color: '#888',
    fontSize: '13px',
    marginTop: '24px',
    textAlign: 'center',
  },
  link: {
    color: '#ff0000',
    textDecoration: 'none',
    fontWeight: '500',
  },
};
