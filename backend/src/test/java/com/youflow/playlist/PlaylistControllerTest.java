package com.youflow.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youflow.auth.JwtUtil;
import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PlaylistController 통합 테스트.
 *
 * 테스트 전략:
 *   - @SpringBootTest: 실제 Spring 컨텍스트 + DB(youflow_test)를 사용해
 *     Controller → Service → Repository 전체 흐름을 검증한다.
 *   - @Transactional: 각 테스트 후 DB를 자동 롤백하여 테스트 간 격리를 보장한다.
 *   - JWT: 실제 JwtUtil로 토큰을 생성해 인증 헤더에 담아 요청한다.
 *
 * 커버 범위:
 *   - GET  /api/playlists       — 목록 조회 (정상, 인증 없음)
 *   - POST /api/playlists       — 생성 (정상, 이름 없음 400)
 *   - DELETE /api/playlists/{id} — 삭제 (정상, 타인 플레이리스트 403, 없는 ID 404)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlaylistControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PlaylistRepository playlistRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    private String token;       // 테스트용 유저 JWT
    private User testUser;      // 테스트용 유저 엔티티

    /**
     * 각 테스트 실행 전:
     *   1. 테스트용 유저를 DB에 저장
     *   2. 해당 유저의 JWT 토큰 발급
     */
    @BeforeEach
    void setUp() {
        testUser = userRepository.save(
                new User("playlist-test@example.com", passwordEncoder.encode("password123")));
        token = jwtUtil.generateToken(testUser.getEmail());
    }

    // ═══════════════════════════════════════════════
    // GET /api/playlists
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 목록 조회 — 플레이리스트 2개 있을 때 200 + 목록 반환")
    void getPlaylists_success() throws Exception {
        // given: 테스트용 플레이리스트 2개 저장
        playlistRepository.save(new Playlist(testUser, "Lo-fi Chill"));
        playlistRepository.save(new Playlist(testUser, "Work BGM"));

        mockMvc.perform(get("/api/playlists")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // JSON 배열 크기 확인
                .andExpect(jsonPath("$", hasSize(2)))
                // 최신순 정렬이므로 나중에 저장한 "Work BGM"이 먼저 올 수 있음
                .andExpect(jsonPath("$[*].name", hasItems("Lo-fi Chill", "Work BGM")));
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 — 인증 토큰 없으면 403 반환")
    void getPlaylists_noAuth_returns403() throws Exception {
        // Authorization 헤더 없이 요청
        mockMvc.perform(get("/api/playlists"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════
    // POST /api/playlists
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 생성 — 정상 생성 시 201 + DTO 반환")
    void createPlaylist_success() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "New Playlist"));

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Playlist"))
                // id와 createdAt이 응답에 포함되는지 확인
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("플레이리스트 생성 — 이름이 빈 문자열이면 400 반환")
    void createPlaylist_blankName_returns400() throws Exception {
        // @NotBlank 유효성 검증이 동작해야 한다
        String body = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("플레이리스트 생성 — name 필드 자체가 없으면 400 반환")
    void createPlaylist_missingName_returns400() throws Exception {
        // 요청 바디에 name 키 자체가 없는 경우
        String body = objectMapper.writeValueAsString(Map.of());

        mockMvc.perform(post("/api/playlists")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ═══════════════════════════════════════════════
    // DELETE /api/playlists/{id}
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 삭제 — 소유자가 삭제 요청 시 204 반환")
    void deletePlaylist_success() throws Exception {
        // given: 테스트용 플레이리스트 저장 후 ID 확보
        Playlist playlist = playlistRepository.save(new Playlist(testUser, "To Delete"));

        mockMvc.perform(delete("/api/playlists/" + playlist.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("플레이리스트 삭제 — 존재하지 않는 ID 요청 시 404 반환")
    void deletePlaylist_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/playlists/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("플레이리스트 삭제 — 다른 유저의 플레이리스트 삭제 시도 시 403 반환")
    void deletePlaylist_notOwner_returns403() throws Exception {
        // given: 다른 유저의 플레이리스트 생성
        User otherUser = userRepository.save(
                new User("other@example.com", passwordEncoder.encode("password123")));
        Playlist otherPlaylist = playlistRepository.save(new Playlist(otherUser, "Other's Playlist"));

        // testUser의 토큰으로 otherUser 플레이리스트 삭제 시도 → 403
        mockMvc.perform(delete("/api/playlists/" + otherPlaylist.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
