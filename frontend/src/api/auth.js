import api from './axios';

/**
 * 인증 관련 API 함수 모음.
 *
 * 서버 엔드포인트:
 *   POST /api/auth/register — 회원가입
 *   POST /api/auth/login    — 로그인
 *
 * 두 함수 모두 성공 시 { token } 을 반환한다.
 * 호출부(authStore)에서 토큰을 localStorage에 저장한다.
 */

/**
 * 회원가입 요청.
 *
 * @param {string} email    이메일 (서버에서 @Email 검증)
 * @param {string} password 비밀번호 (8자 이상, 서버에서 @Size 검증)
 * @returns {Promise<{token: string}>} 발급된 JWT 토큰
 * @throws 409 — 이미 존재하는 이메일
 * @throws 400 — 이메일 형식 오류 또는 비밀번호 8자 미만
 */
export const register = async (email, password) => {
  const response = await api.post('/auth/register', { email, password });
  return response.data; // { token }
};

/**
 * 로그인 요청.
 *
 * @param {string} email    이메일
 * @param {string} password 비밀번호
 * @returns {Promise<{token: string}>} 발급된 JWT 토큰
 * @throws 401 — 이메일 없음 또는 비밀번호 불일치
 */
export const login = async (email, password) => {
  const response = await api.post('/auth/login', { email, password });
  return response.data; // { token }
};
