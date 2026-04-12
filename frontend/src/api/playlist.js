import api from './axios';

/**
 * 플레이리스트 관련 API 함수 모음.
 *
 * 서버 엔드포인트:
 *   GET    /api/playlists       — 내 플레이리스트 목록 조회
 *   POST   /api/playlists       — 플레이리스트 생성
 *   DELETE /api/playlists/{id}  — 플레이리스트 삭제
 *
 * 모든 요청에 axios 인터셉터가 JWT 헤더를 자동 첨부한다.
 */

/**
 * 내 플레이리스트 목록을 최신순으로 조회한다.
 *
 * @returns {Promise<Array<{id, name, createdAt}>>}
 */
export const getPlaylists = async () => {
  const response = await api.get('/playlists');
  return response.data;
};

/**
 * 새 플레이리스트를 생성한다.
 *
 * @param {string} name 플레이리스트 이름
 * @returns {Promise<{id, name, createdAt}>} 생성된 플레이리스트
 * @throws 400 — 이름이 비어있을 때
 */
export const createPlaylist = async (name) => {
  const response = await api.post('/playlists', { name });
  return response.data;
};

/**
 * 플레이리스트를 삭제한다.
 *
 * @param {number} playlistId 삭제할 플레이리스트 ID
 * @throws 403 — 본인 소유가 아닐 때
 * @throws 404 — 존재하지 않는 ID
 */
export const deletePlaylist = async (playlistId) => {
  await api.delete(`/playlists/${playlistId}`);
};
