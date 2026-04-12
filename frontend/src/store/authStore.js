import { create } from 'zustand';
import { login as loginApi, register as registerApi } from '../api/auth';

/**
 * 인증 상태 스토어.
 *
 * 관리하는 상태:
 *   - token: JWT 토큰 (localStorage와 동기화)
 *   - email: 로그인한 유저의 이메일 (WebSocket 룸 ID로 사용)
 *   - isAuthenticated: 로그인 여부 (컴포넌트에서 분기 처리용)
 *
 * 초기화 전략:
 *   앱 시작 시 localStorage에 토큰이 있으면 자동으로 로그인 상태로 복원한다.
 *   토큰의 유효성은 첫 API 요청 시 서버가 검증한다(401 → 자동 로그아웃).
 *
 * WebSocket 룸과의 연결:
 *   email이 룸 ID이므로, 이 스토어의 email 값을 useWebSocket 훅에서 사용한다.
 */

/** localStorage에서 초기 토큰을 읽어 email을 파싱한다. */
const getInitialState = () => {
  const token = localStorage.getItem('token');
  if (!token) return { token: null, email: null, isAuthenticated: false };

  try {
    // JWT payload는 Base64 인코딩된 JSON
    // 형식: header.payload.signature
    const payload = JSON.parse(atob(token.split('.')[1]));
    return {
      token,
      email: payload.sub, // Spring Security의 기본 subject 필드 = email
      isAuthenticated: true,
    };
  } catch {
    // 토큰이 손상된 경우 초기 상태로 리셋
    localStorage.removeItem('token');
    return { token: null, email: null, isAuthenticated: false };
  }
};

const useAuthStore = create((set) => ({
  ...getInitialState(),

  /**
   * 로그인 처리.
   * 서버에서 받은 토큰을 localStorage와 스토어에 동시에 저장한다.
   *
   * @param {string} email
   * @param {string} password
   */
  login: async (email, password) => {
    const { token } = await loginApi(email, password);
    localStorage.setItem('token', token);
    set({ token, email, isAuthenticated: true });
  },

  /**
   * 회원가입 처리.
   * 가입 성공 후 바로 로그인 상태로 전환한다.
   *
   * @param {string} email
   * @param {string} password
   */
  register: async (email, password) => {
    const { token } = await registerApi(email, password);
    localStorage.setItem('token', token);
    set({ token, email, isAuthenticated: true });
  },

  /**
   * 로그아웃 처리.
   * localStorage 토큰 삭제 및 스토어 초기화.
   */
  logout: () => {
    localStorage.removeItem('token');
    set({ token: null, email: null, isAuthenticated: false });
  },
}));

export default useAuthStore;
