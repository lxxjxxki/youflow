import api from './axios';

/**
 * 영상 관련 API 함수 모음.
 *
 * 서버 엔드포인트:
 *   GET    /api/playlists/{playlistId}/videos                       — 영상 목록 조회
 *   POST   /api/playlists/{playlistId}/videos                       — 영상 추가
 *   DELETE /api/playlists/{playlistId}/videos/{videoId}             — 영상 삭제
 *   PATCH  /api/playlists/{playlistId}/videos/{videoId}/volume      — 음량 업데이트
 */

/**
 * 플레이리스트의 영상 목록을 재생 순서(position ASC)로 조회한다.
 *
 * @param {number} playlistId 조회할 플레이리스트 ID
 * @returns {Promise<Array<{id, youtubeId, title, thumbnail, position, volume, createdAt}>>}
 */
export const getVideos = async (playlistId) => {
  const response = await api.get(`/playlists/${playlistId}/videos`);
  return response.data;
};

/**
 * 플레이리스트에 YouTube 영상을 추가한다.
 * 서버에서 YouTube API를 호출해 title/thumbnail을 자동으로 채운다.
 *
 * @param {number} playlistId  영상을 추가할 플레이리스트 ID
 * @param {string} youtubeUrl  유저가 입력한 YouTube URL
 * @returns {Promise<{id, youtubeId, title, thumbnail, position, volume, createdAt}>}
 * @throws 400 — 유효하지 않은 YouTube URL
 * @throws 404 — YouTube에 존재하지 않는 영상
 */
export const addVideo = async (playlistId, youtubeUrl) => {
  const response = await api.post(`/playlists/${playlistId}/videos`, { youtubeUrl });
  return response.data;
};

/**
 * 플레이리스트에서 영상을 삭제한다.
 *
 * @param {number} playlistId 플레이리스트 ID
 * @param {number} videoId    삭제할 영상 ID
 */
export const removeVideo = async (playlistId, videoId) => {
  await api.delete(`/playlists/${playlistId}/videos/${videoId}`);
};

/**
 * 영상의 음량을 업데이트한다.
 * 유저가 볼륨 슬라이더를 조절할 때 호출되어 DB에 즉시 반영된다.
 * 이후 해당 영상 재생 시 저장된 volume이 iframe에 자동 적용된다.
 *
 * @param {number} playlistId 플레이리스트 ID
 * @param {number} videoId    음량을 변경할 영상 ID
 * @param {number} volume     새 음량 (0~100)
 * @returns {Promise<{id, youtubeId, title, thumbnail, position, volume, createdAt}>}
 */
export const updateVolume = async (playlistId, videoId, volume) => {
  const response = await api.patch(
    `/playlists/${playlistId}/videos/${videoId}/volume`,
    { volume }
  );
  return response.data;
};
