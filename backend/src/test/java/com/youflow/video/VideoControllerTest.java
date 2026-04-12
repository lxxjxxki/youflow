package com.youflow.video;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youflow.auth.JwtUtil;
import com.youflow.playlist.Playlist;
import com.youflow.playlist.PlaylistRepository;
import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VideoController 통합 테스트.
 *
 * 테스트 전략:
 *   - @SpringBootTest: 실제 Spring 컨텍스트 + DB(youflow_test)로 전체 흐름 검증
 *   - @MockBean YouTubeApiClient: 실제 YouTube API 호출 없이 Mock 응답 사용
 *     → CI/CD 환경에서 API 키 없이도 테스트 가능
 *   - @Transactional: 각 테스트 후 DB 자동 롤백
 *
 * 커버 범위:
 *   - GET    /api/playlists/{id}/videos              — 목록 조회
 *   - POST   /api/playlists/{id}/videos              — 영상 추가 (정상, 잘못된 URL 400)
 *   - DELETE /api/playlists/{id}/videos/{videoId}   — 영상 삭제 (정상, 타인 403)
 *   - PATCH  /api/playlists/{id}/videos/{videoId}/volume — 음량 업데이트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VideoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PlaylistRepository playlistRepository;
    @Autowired VideoRepository videoRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    /** 실제 YouTube API 호출 대신 Mock 응답 반환 */
    @MockBean YouTubeApiClient youTubeApiClient;

    private String token;
    private User testUser;
    private Playlist testPlaylist;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
                new User("video-test@example.com", passwordEncoder.encode("password123")));
        testPlaylist = playlistRepository.save(new Playlist(testUser, "Test Playlist"));
        token = jwtUtil.generateToken(testUser.getEmail());

        // YouTube API Mock: 어떤 영상 ID든 같은 메타데이터 반환
        given(youTubeApiClient.fetchMeta(anyString()))
                .willReturn(new YouTubeApiClient.VideoMeta("Mock Title", "https://mock-thumb.jpg"));
    }

    // ═══════════════════════════════════════════════
    // GET /api/playlists/{playlistId}/videos
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 목록 조회 — 영상 2개 있을 때 200 + 목록 반환")
    void getVideos_success() throws Exception {
        // given: 영상 2개 저장
        videoRepository.save(new Video(testPlaylist,
                "https://youtu.be/aaa11111111", "aaa11111111", "Title1", "thumb1", 0));
        videoRepository.save(new Video(testPlaylist,
                "https://youtu.be/bbb22222222", "bbb22222222", "Title2", "thumb2", 1));

        mockMvc.perform(get("/api/playlists/" + testPlaylist.getId() + "/videos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // position ASC 정렬 확인
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    // ═══════════════════════════════════════════════
    // POST /api/playlists/{playlistId}/videos
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 추가 — 정상 추가 시 201 + DTO 반환")
    void addVideo_success() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("youtubeUrl", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"));

        mockMvc.perform(post("/api/playlists/" + testPlaylist.getId() + "/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.youtubeId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.title").value("Mock Title"))
                .andExpect(jsonPath("$.volume").value(70))   // 기본 음량 확인
                .andExpect(jsonPath("$.position").value(0)); // 첫 영상이므로 0
    }

    @Test
    @DisplayName("영상 추가 — 유효하지 않은 YouTube URL이면 400 반환")
    void addVideo_invalidUrl_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("youtubeUrl", "https://vimeo.com/123456"));

        mockMvc.perform(post("/api/playlists/" + testPlaylist.getId() + "/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("영상 추가 — youtubeUrl 필드 없으면 400 반환")
    void addVideo_missingUrl_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of());

        mockMvc.perform(post("/api/playlists/" + testPlaylist.getId() + "/videos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════
    // DELETE /api/playlists/{playlistId}/videos/{videoId}
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 삭제 — 정상 삭제 시 204 반환")
    void removeVideo_success() throws Exception {
        Video video = videoRepository.save(new Video(testPlaylist,
                "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", "Title", "thumb", 0));

        mockMvc.perform(delete("/api/playlists/" + testPlaylist.getId() + "/videos/" + video.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("영상 삭제 — 다른 유저의 플레이리스트 영상 삭제 시도 시 403 반환")
    void removeVideo_notOwner_returns403() throws Exception {
        // 다른 유저와 그 플레이리스트 생성
        User otherUser = userRepository.save(
                new User("other@example.com", passwordEncoder.encode("password123")));
        Playlist otherPlaylist = playlistRepository.save(new Playlist(otherUser, "Other Playlist"));
        Video video = videoRepository.save(new Video(otherPlaylist,
                "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", "Title", "thumb", 0));

        // testUser 토큰으로 다른 유저의 영상 삭제 시도
        mockMvc.perform(delete("/api/playlists/" + otherPlaylist.getId() + "/videos/" + video.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════
    // PATCH /api/playlists/{playlistId}/videos/{videoId}/volume
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("음량 업데이트 — 정상 업데이트 시 200 + 업데이트된 DTO 반환")
    void updateVolume_success() throws Exception {
        Video video = videoRepository.save(new Video(testPlaylist,
                "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", "Title", "thumb", 0));

        String body = objectMapper.writeValueAsString(Map.of("volume", 85));

        mockMvc.perform(patch("/api/playlists/" + testPlaylist.getId() + "/videos/" + video.getId() + "/volume")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume").value(85));
    }

    @Test
    @DisplayName("음량 업데이트 — 100 초과 값이면 400 반환")
    void updateVolume_outOfRange_returns400() throws Exception {
        Video video = videoRepository.save(new Video(testPlaylist,
                "https://youtu.be/dQw4w9WgXcQ", "dQw4w9WgXcQ", "Title", "thumb", 0));

        String body = objectMapper.writeValueAsString(Map.of("volume", 150));

        mockMvc.perform(patch("/api/playlists/" + testPlaylist.getId() + "/videos/" + video.getId() + "/volume")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
