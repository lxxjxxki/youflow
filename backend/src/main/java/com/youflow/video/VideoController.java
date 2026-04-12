package com.youflow.video;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 영상 REST API 컨트롤러.
 *
 * 모든 엔드포인트는 JWT 인증 필수.
 * 플레이리스트 ID는 URL 경로에 포함되어 소유권 검증이 Service에서 수행된다.
 *
 * 엔드포인트:
 *   GET    /api/playlists/{playlistId}/videos              — 영상 목록 조회
 *   POST   /api/playlists/{playlistId}/videos              — 영상 추가
 *   DELETE /api/playlists/{playlistId}/videos/{videoId}   — 영상 삭제
 *   PATCH  /api/playlists/{playlistId}/videos/{videoId}/volume — 음량 업데이트
 */
@RestController
@RequestMapping("/api/playlists/{playlistId}/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 플레이리스트의 영상 목록을 재생 순서(position ASC)로 반환한다.
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param playlistId  조회할 플레이리스트 ID
     * @return 영상 DTO 목록 (없으면 빈 배열 [])
     */
    @GetMapping
    public List<VideoResponse> getVideos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long playlistId) {

        return videoService.getVideos(userDetails.getUsername(), playlistId);
    }

    /**
     * 플레이리스트에 YouTube 영상을 추가한다.
     *
     * 요청 바디 예시:
     * {
     *   "youtubeUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
     * }
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param playlistId  영상을 추가할 플레이리스트 ID
     * @param request     YouTube URL이 담긴 요청 DTO
     * @return 추가된 영상 DTO (HTTP 201 Created)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse addVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long playlistId,
            @Valid @RequestBody AddVideoRequest request) {

        return videoService.addVideo(userDetails.getUsername(), playlistId, request.youtubeUrl());
    }

    /**
     * 플레이리스트에서 영상을 삭제한다.
     * 삭제 후 후속 영상의 position이 자동으로 재정렬된다.
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param playlistId  플레이리스트 ID (소유권 검증용)
     * @param videoId     삭제할 영상 ID
     */
    @DeleteMapping("/{videoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVideo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long playlistId,
            @PathVariable Long videoId) {

        videoService.removeVideo(userDetails.getUsername(), playlistId, videoId);
    }

    /**
     * 영상의 음량을 업데이트한다.
     * 유저가 슬라이더를 조절할 때마다 호출되어 DB에 즉시 반영된다.
     *
     * 요청 바디 예시:
     * {
     *   "volume": 85
     * }
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param playlistId  플레이리스트 ID (소유권 검증용)
     * @param videoId     음량을 변경할 영상 ID
     * @param request     새 음량 값이 담긴 요청 DTO
     * @return 업데이트된 영상 DTO
     */
    @PatchMapping("/{videoId}/volume")
    public VideoResponse updateVolume(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long playlistId,
            @PathVariable Long videoId,
            @Valid @RequestBody UpdateVolumeRequest request) {

        return videoService.updateVolume(userDetails.getUsername(), videoId, request.volume());
    }

    // ─────────────────────────────────────────────
    // 요청 DTO
    // ─────────────────────────────────────────────

    /**
     * 영상 추가 요청 DTO.
     *
     * @NotBlank: null, 빈 문자열, 공백만 있는 문자열 모두 거부.
     * URL 형식 유효성은 Service의 parseYoutubeId()에서 검증한다.
     */
    public record AddVideoRequest(
            @NotBlank(message = "YouTube URL은 필수입니다") String youtubeUrl
    ) {}

    /**
     * 음량 업데이트 요청 DTO.
     *
     * @Min(0) @Max(100): 0~100 범위 외 값은 400으로 자동 거부.
     */
    public record UpdateVolumeRequest(
            @Min(value = 0, message = "음량은 0 이상이어야 합니다")
            @Max(value = 100, message = "음량은 100 이하여야 합니다")
            int volume
    ) {}
}
