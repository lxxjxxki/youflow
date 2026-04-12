package com.youflow.video;

import com.youflow.playlist.Playlist;
import com.youflow.playlist.PlaylistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영상 비즈니스 로직 담당 서비스.
 *
 * 주요 책임:
 *   1. YouTube URL에서 영상 ID(youtubeId) 파싱
 *   2. YouTubeApiClient를 통해 title/thumbnail 조회
 *   3. 플레이리스트 소유권 검증 (다른 유저의 플레이리스트에 영상 추가/삭제 방지)
 *   4. position 자동 관리 (추가 시 max+1, 삭제 시 후속 영상 position 재정렬)
 *   5. 음량(volume) 업데이트
 */
@Service
@Transactional(readOnly = true)
public class VideoService {

    /**
     * 지원하는 YouTube URL 형식:
     *   - https://www.youtube.com/watch?v=VIDEO_ID
     *   - https://youtu.be/VIDEO_ID
     *   - https://www.youtube.com/embed/VIDEO_ID
     *
     * 정규식 그룹 1이 추출된 VIDEO_ID다.
     */
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/)|youtu\\.be/)([A-Za-z0-9_-]{11})"
    );

    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    private final YouTubeApiClient youTubeApiClient;

    public VideoService(VideoRepository videoRepository,
                        PlaylistRepository playlistRepository,
                        YouTubeApiClient youTubeApiClient) {
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
        this.youTubeApiClient = youTubeApiClient;
    }

    /**
     * 플레이리스트의 영상 목록을 재생 순서(position ASC)로 반환한다.
     *
     * @param email      로그인한 유저의 이메일 (소유권 검증용)
     * @param playlistId 조회할 플레이리스트 ID
     * @return 영상 DTO 목록 (없으면 빈 리스트)
     */
    public List<VideoResponse> getVideos(String email, Long playlistId) {
        // 플레이리스트 존재 여부 + 소유권 동시 검증
        validateOwnership(email, playlistId);

        return videoRepository.findByPlaylistIdOrderByPositionAsc(playlistId)
                .stream()
                .map(VideoResponse::from)
                .toList();
    }

    /**
     * 플레이리스트에 영상을 추가한다.
     *
     * 흐름:
     *   1. 플레이리스트 소유권 검증
     *   2. URL에서 youtubeId 파싱
     *   3. YouTube API로 title/thumbnail 조회
     *   4. 현재 최대 position + 1로 새 영상 저장
     *
     * @param email      로그인한 유저의 이메일
     * @param playlistId 영상을 추가할 플레이리스트 ID
     * @param youtubeUrl 유저가 입력한 YouTube URL
     * @return 추가된 영상 DTO
     */
    @Transactional
    public VideoResponse addVideo(String email, Long playlistId, String youtubeUrl) {
        Playlist playlist = validateOwnership(email, playlistId);

        // URL에서 youtubeId 추출
        String youtubeId = parseYoutubeId(youtubeUrl);

        // YouTube API로 메타데이터(제목, 썸네일) 조회
        YouTubeApiClient.VideoMeta meta = youTubeApiClient.fetchMeta(youtubeId);

        // 현재 최대 position + 1 (영상이 없으면 0부터 시작)
        Integer maxPosition = videoRepository.findMaxPositionByPlaylistId(playlistId);
        int nextPosition = (maxPosition == null) ? 0 : maxPosition + 1;

        Video video = new Video(playlist, youtubeUrl, youtubeId,
                meta.title(), meta.thumbnail(), nextPosition);
        Video saved = videoRepository.save(video);

        return VideoResponse.from(saved);
    }

    /**
     * 영상을 삭제한다. 삭제 후 후속 영상의 position을 재정렬한다.
     *
     * @param email      로그인한 유저의 이메일
     * @param playlistId 플레이리스트 ID (소유권 검증용)
     * @param videoId    삭제할 영상 ID
     */
    @Transactional
    public void removeVideo(String email, Long playlistId, Long videoId) {
        validateOwnership(email, playlistId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        // 영상이 해당 플레이리스트에 속하는지 확인
        if (!video.getPlaylist().getId().equals(playlistId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video does not belong to this playlist");
        }

        int deletedPosition = video.getPosition();
        videoRepository.delete(video);

        // 삭제된 position 이후 영상들의 position을 1씩 당긴다
        List<Video> remaining = videoRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        for (Video v : remaining) {
            if (v.getPosition() > deletedPosition) {
                v.updatePosition(v.getPosition() - 1);
            }
        }
        // @Transactional이므로 dirty checking으로 자동 UPDATE 발생
    }

    /**
     * 영상의 음량을 업데이트한다.
     * WebSocket 또는 REST API로 유저가 볼륨을 조절할 때 호출된다.
     *
     * @param email    로그인한 유저의 이메일
     * @param videoId  음량을 변경할 영상 ID
     * @param volume   새 음량 값 (0~100)
     * @return 업데이트된 영상 DTO
     */
    @Transactional
    public VideoResponse updateVolume(String email, Long videoId, int volume) {
        if (volume < 0 || volume > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Volume must be between 0 and 100");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));

        // 이 영상이 속한 플레이리스트의 소유자인지 검증
        validateOwnership(email, video.getPlaylist().getId());

        video.updateVolume(volume);
        // @Transactional dirty checking으로 자동 UPDATE — save() 불필요
        return VideoResponse.from(video);
    }

    // ─────────────────────────────────────────────
    // private 헬퍼 메서드
    // ─────────────────────────────────────────────

    /**
     * 플레이리스트 존재 여부와 소유권을 동시에 검증한다.
     *
     * @param email      검증할 유저의 이메일
     * @param playlistId 검증할 플레이리스트 ID
     * @return 검증된 Playlist 엔티티
     * @throws ResponseStatusException 404 — 플레이리스트가 존재하지 않을 때
     * @throws ResponseStatusException 403 — 요청자가 소유자가 아닐 때
     */
    private Playlist validateOwnership(String email, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));

        if (!playlist.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your playlist");
        }

        return playlist;
    }

    /**
     * YouTube URL에서 11자리 영상 ID를 추출한다.
     *
     * 지원 형식:
     *   - https://www.youtube.com/watch?v=dQw4w9WgXcQ
     *   - https://youtu.be/dQw4w9WgXcQ
     *   - https://www.youtube.com/embed/dQw4w9WgXcQ
     *
     * @param url 파싱할 YouTube URL
     * @return 11자리 YouTube 영상 ID
     * @throws ResponseStatusException 400 — 유효하지 않은 YouTube URL일 때
     */
    String parseYoutubeId(String url) {
        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "유효하지 않은 YouTube URL입니다: " + url);
        }
        return matcher.group(1);
    }
}
