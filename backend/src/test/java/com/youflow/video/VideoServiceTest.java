package com.youflow.video;

import com.youflow.playlist.Playlist;
import com.youflow.playlist.PlaylistRepository;
import com.youflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * VideoService 단위 테스트.
 *
 * 테스트 전략:
 *   - Mockito로 Repository와 YouTubeApiClient를 Mock 처리 → DB/HTTP 없이 빠른 검증
 *   - DB 연동 통합 테스트는 VideoControllerTest에서 진행
 *
 * 커버 범위:
 *   - parseYoutubeId: 유효한 URL 3가지, 잘못된 URL
 *   - addVideo: 정상 추가, 플레이리스트 없음(404), 소유권 불일치(403)
 *   - removeVideo: 정상 삭제, 영상 없음(404)
 *   - updateVolume: 정상 업데이트, 범위 초과(400)
 */
@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock private VideoRepository videoRepository;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private YouTubeApiClient youTubeApiClient;

    @InjectMocks private VideoService videoService;

    private User testUser;
    private Playlist testPlaylist;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedPassword");
        testPlaylist = new Playlist(testUser, "Test Playlist");
        // JPA 없이 생성된 객체는 id = null이다.
        // VideoService.removeVideo()에서 video.getPlaylist().getId().equals(playlistId)를
        // 호출하므로 NPE 방지를 위해 ReflectionTestUtils로 id를 직접 주입한다.
        ReflectionTestUtils.setField(testPlaylist, "id", 1L);
    }

    // ═══════════════════════════════════════════════
    // parseYoutubeId — URL 파싱 로직 단독 검증
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("YouTube URL 파싱 — watch?v= 형식")
    void parseYoutubeId_watchUrl() {
        String id = videoService.parseYoutubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertThat(id).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("YouTube URL 파싱 — youtu.be 단축 형식")
    void parseYoutubeId_shortUrl() {
        String id = videoService.parseYoutubeId("https://youtu.be/dQw4w9WgXcQ");
        assertThat(id).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("YouTube URL 파싱 — embed 형식")
    void parseYoutubeId_embedUrl() {
        String id = videoService.parseYoutubeId("https://www.youtube.com/embed/dQw4w9WgXcQ");
        assertThat(id).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("YouTube URL 파싱 — 유효하지 않은 URL이면 400 예외")
    void parseYoutubeId_invalidUrl_throws400() {
        assertThatThrownBy(() -> videoService.parseYoutubeId("https://vimeo.com/123456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // ═══════════════════════════════════════════════
    // addVideo
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 추가 — 정상 추가 후 DTO 반환")
    void addVideo_success() {
        // given
        given(playlistRepository.findById(1L)).willReturn(Optional.of(testPlaylist));
        given(youTubeApiClient.fetchMeta("dQw4w9WgXcQ"))
                .willReturn(new YouTubeApiClient.VideoMeta("Rick Astley", "https://thumb.jpg"));
        given(videoRepository.findMaxPositionByPlaylistId(1L)).willReturn(null); // 첫 영상
        given(videoRepository.save(any(Video.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        VideoResponse result = videoService.addVideo(
                "test@example.com", 1L, "https://www.youtube.com/watch?v=dQw4w9WgXcQ");

        // then
        assertThat(result.youtubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.title()).isEqualTo("Rick Astley");
        assertThat(result.position()).isEqualTo(0); // 첫 영상이므로 position = 0
    }

    @Test
    @DisplayName("영상 추가 — 기존 영상이 있으면 position이 max+1")
    void addVideo_positionIsMaxPlusOne() {
        // given: 현재 최대 position = 2
        given(playlistRepository.findById(1L)).willReturn(Optional.of(testPlaylist));
        given(youTubeApiClient.fetchMeta(any())).willReturn(
                new YouTubeApiClient.VideoMeta("Title", "https://thumb.jpg"));
        given(videoRepository.findMaxPositionByPlaylistId(1L)).willReturn(2);
        given(videoRepository.save(any(Video.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        VideoResponse result = videoService.addVideo(
                "test@example.com", 1L, "https://youtu.be/dQw4w9WgXcQ");

        // then: position = 3 (max 2 + 1)
        assertThat(result.position()).isEqualTo(3);
    }

    @Test
    @DisplayName("영상 추가 — 플레이리스트 없으면 404 예외")
    void addVideo_playlistNotFound_throws404() {
        given(playlistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                videoService.addVideo("test@example.com", 99L, "https://youtu.be/dQw4w9WgXcQ"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("영상 추가 — 다른 유저의 플레이리스트에 추가 시도 시 403 예외")
    void addVideo_notOwner_throws403() {
        given(playlistRepository.findById(1L)).willReturn(Optional.of(testPlaylist));

        assertThatThrownBy(() ->
                videoService.addVideo("hacker@example.com", 1L, "https://youtu.be/dQw4w9WgXcQ"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ═══════════════════════════════════════════════
    // removeVideo
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("영상 삭제 — 정상 삭제")
    void removeVideo_success() {
        // given
        given(playlistRepository.findById(1L)).willReturn(Optional.of(testPlaylist));
        Video video = new Video(testPlaylist, "https://youtu.be/abc", "abc",
                "Title", "thumb", 0);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));
        given(videoRepository.findByPlaylistIdOrderByPositionAsc(1L)).willReturn(List.of());

        // when
        videoService.removeVideo("test@example.com", 1L, 1L);

        // then
        then(videoRepository).should().delete(video);
    }

    @Test
    @DisplayName("영상 삭제 — 존재하지 않는 영상 ID면 404 예외")
    void removeVideo_notFound_throws404() {
        given(playlistRepository.findById(1L)).willReturn(Optional.of(testPlaylist));
        given(videoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                videoService.removeVideo("test@example.com", 1L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ═══════════════════════════════════════════════
    // updateVolume
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("음량 업데이트 — 정상 업데이트 후 DTO 반환")
    void updateVolume_success() {
        // given
        Video video = new Video(testPlaylist, "https://youtu.be/abc", "abc",
                "Title", "thumb", 0);
        given(videoRepository.findById(1L)).willReturn(Optional.of(video));
        given(playlistRepository.findById(any())).willReturn(Optional.of(testPlaylist));

        // when
        VideoResponse result = videoService.updateVolume("test@example.com", 1L, 85);

        // then
        assertThat(result.volume()).isEqualTo(85);
    }

    @Test
    @DisplayName("음량 업데이트 — 100 초과 값이면 400 예외")
    void updateVolume_outOfRange_throws400() {
        assertThatThrownBy(() ->
                videoService.updateVolume("test@example.com", 1L, 150))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }
}
