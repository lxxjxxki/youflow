import axios from 'axios';

/**
 * 공용 axios 인스턴스.
 *
 * 모든 API 함수는 이 인스턴스를 사용한다.
 * 직접 axios.get/post를 호출하지 않는다.
 *
 * 인터셉터 역할:
 *   - 요청(request): localStorage에서 JWT 토큰을 읽어
 *     Authorization 헤더에 자동으로 첨부한다.
 *     덕분에 각 API 함수마다 헤더를 직접 설정할 필요가 없다.
 *   - 응답(response): 401 Unauthorized 발생 시
 *     토큰을 삭제하고 로그인 페이지로 리다이렉트한다.
 */
const api = axios.create({
  baseURL: '/api', // Vite proxy 설정으로 개발 중 CORS 우회
  headers: {
    'Content-Type': 'application/json',
  },
});

// ─────────────────────────────────────────────
// 요청 인터셉터 — JWT 토큰 자동 첨부
// ─────────────────────────────────────────────
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─────────────────────────────────────────────
// 응답 인터셉터 — 401 처리
// ─────────────────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 만료 또는 무효 — 로컬 상태 초기화 후 로그인 페이지로 이동
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
