package com.youflow.playlist;

import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * PlaylistService 단위 테스트.
 *
 * 테스트 전략:
 *   - @SpringBootTest 없이 Mockito만 사용 → DB 불필요, 속도 빠름
 *   - Repository를 Mock으로 교체해 비즈니스 로직(검증, 예외)만 집중 검증
 *   - DB 연동 통합 테스트는 PlaylistControllerTest에서 별도 진행
 *
 * 커버 범위:
 *   - getPlaylists: 정상 조회, 빈 목록
 *   - createPlaylist: 정상 생성, 유저 없음(404)
 *   - deletePlaylist: 정상 삭제, 플레이리스트 없음(404), 소유자 불일치(403)
 */
@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private UserRepository userRepository;

    /** @Mock 필드를 생성자 주입 방식으로 PlaylistService에 자동 주입 */
    @InjectMocks
    private PlaylistService playlistService;

    /** 테스트 전체에서 재사용할 유저 픽스처 */
    private User testUser;

    @BeforeEach
    void setUp() {
        // DB 없이 순수 Java 객체로 유저 픽스처 생성
        testUser = new User("test@example.com", "hashedPassword");
    }

    // ═══════════════════════════════════════════════
    // getPlaylists
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 목록 조회 — 2개 있을 때 DTO 목록으로 반환")
    void getPlaylists_returnsList() {
        // given: Repository가 플레이리스트 2개를 반환하도록 설정
        Playlist p1 = new Playlist(testUser, "Lo-fi Chill");
        Playlist p2 = new Playlist(testUser, "Work BGM");
        given(playlistRepository.findByUserEmailOrderByCreatedAtDesc("test@example.com"))
                .willReturn(List.of(p1, p2));

        // when
        List<PlaylistResponse> result = playlistService.getPlaylists("test@example.com");

        // then: 크기와 이름이 올바르게 변환되었는지 확인
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Lo-fi Chill");
        assertThat(result.get(1).name()).isEqualTo("Work BGM");
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 — 플레이리스트가 없으면 빈 리스트 반환")
    void getPlaylists_empty_returnsEmptyList() {
        // given
        given(playlistRepository.findByUserEmailOrderByCreatedAtDesc("test@example.com"))
                .willReturn(List.of());

        // when
        List<PlaylistResponse> result = playlistService.getPlaylists("test@example.com");

        // then: null이 아닌 빈 리스트여야 한다
        assertThat(result).isNotNull().isEmpty();
    }

    // ═══════════════════════════════════════════════
    // createPlaylist
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 생성 — 정상 생성 후 DTO 반환")
    void createPlaylist_success() {
        // given
        given(userRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(testUser));

        // save()가 받은 Playlist 객체를 그대로 반환하도록 설정
        given(playlistRepository.save(any(Playlist.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        PlaylistResponse result = playlistService.createPlaylist("test@example.com", "My Playlist");

        // then: 이름이 정확히 담겼는지, save가 실제로 호출되었는지 확인
        assertThat(result.name()).isEqualTo("My Playlist");
        then(playlistRepository).should().save(any(Playlist.class));
    }

    @Test
    @DisplayName("플레이리스트 생성 — 존재하지 않는 유저 이메일이면 404 예외")
    void createPlaylist_userNotFound_throws404() {
        // given: UserRepository가 Optional.empty() 반환
        given(userRepository.findByEmail("ghost@example.com"))
                .willReturn(Optional.empty());

        // when & then: 404 상태의 ResponseStatusException이 발생해야 한다
        assertThatThrownBy(() ->
                playlistService.createPlaylist("ghost@example.com", "My Playlist"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ═══════════════════════════════════════════════
    // deletePlaylist
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("플레이리스트 삭제 — 소유자가 요청하면 정상 삭제")
    void deletePlaylist_success() {
        // given: 소유자가 testUser인 플레이리스트
        Playlist playlist = new Playlist(testUser, "To Delete");
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        // when
        playlistService.deletePlaylist("test@example.com", 1L);

        // then: delete()가 해당 플레이리스트로 정확히 한 번 호출되었는지 검증
        then(playlistRepository).should().delete(playlist);
    }

    @Test
    @DisplayName("플레이리스트 삭제 — 존재하지 않는 ID면 404 예외")
    void deletePlaylist_notFound_throws404() {
        // given
        given(playlistRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                playlistService.deletePlaylist("test@example.com", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("플레이리스트 삭제 — 다른 유저가 요청하면 403 예외 (소유권 검증)")
    void deletePlaylist_notOwner_throws403() {
        // given: 플레이리스트 소유자는 test@example.com,
        //        요청자는 hacker@example.com → 403이 발생해야 한다
        Playlist playlist = new Playlist(testUser, "Someone's Playlist");
        given(playlistRepository.findById(1L)).willReturn(Optional.of(playlist));

        // when & then
        assertThatThrownBy(() ->
                playlistService.deletePlaylist("hacker@example.com", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }
}
