import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import useAuthStore from '../store/authStore';

/**
 * 회원가입 페이지.
 *
 * 흐름:
 *   1. 이메일 / 비밀번호 / 비밀번호 확인 입력
 *   2. 클라이언트 측 유효성 검사 (비밀번호 8자 이상, 확인 일치)
 *   3. authStore.register() 호출 → 서버 POST /api/auth/register
 *   4. 성공: JWT 저장 후 /controller로 이동
 *   5. 실패: 서버 에러 메시지를 폼 하단에 표시
 *
 * 참고: 서버에서도 @Email, @Size(min=8) 검증을 하지만,
 *       클라이언트에서 먼저 검사해서 불필요한 네트워크 요청을 줄인다.
 */
export default function RegisterPage() {
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  /** 클라이언트 측 유효성 검사. 오류 시 메시지 반환, 통과 시 null 반환. */
  const validate = () => {
    if (password.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
    if (password !== passwordConfirm) return '비밀번호가 일치하지 않습니다.';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsLoading(true);
    try {
      await register(email, password);
      navigate('/controller');
    } catch (err) {
      const status = err.response?.status;
      if (status === 409) {
        setError('이미 사용 중인 이메일입니다.');
      } else if (status === 400) {
        setError('입력 형식을 확인해주세요.');
      } else {
        setError('회원가입 중 오류가 발생했습니다. 다시 시도해주세요.');
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
        <p style={styles.subtitle}>새 계정 만들기</p>

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
              placeholder="8자 이상"
              required
              style={styles.input}
            />
          </div>

          {/* 비밀번호 확인 */}
          <div style={styles.field}>
            <label style={styles.label}>비밀번호 확인</label>
            <input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              placeholder="비밀번호 재입력"
              required
              style={styles.input}
            />
          </div>

          {/* 에러 메시지 */}
          {error && <p style={styles.error}>{error}</p>}

          {/* 가입 버튼 */}
          <button
            type="submit"
            disabled={isLoading}
            style={{ ...styles.button, opacity: isLoading ? 0.7 : 1 }}
          >
            {isLoading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        {/* 로그인 링크 */}
        <p style={styles.footer}>
          이미 계정이 있으신가요?{' '}
          <Link to="/login" style={styles.link}>
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#0f0f0f',
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
    color: '#ff0000',
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
