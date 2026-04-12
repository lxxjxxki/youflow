package com.youflow.playlist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 플레이리스트 REST API 컨트롤러.
 *
 * 모든 엔드포인트는 JWT 인증 필수 (SecurityConfig에서 /api/auth/** 외 전부 인증 요구).
 * 인증된 유저 정보는 @AuthenticationPrincipal로 주입받는다.
 *
 * 엔드포인트:
 *   GET    /api/playlists       — 내 플레이리스트 목록 조회
 *   POST   /api/playlists       — 플레이리스트 생성
 *   DELETE /api/playlists/{id}  — 플레이리스트 삭제
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /**
     * 로그인한 유저의 플레이리스트 목록을 최신순으로 반환한다.
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @return 플레이리스트 DTO 목록 (없으면 빈 배열 [])
     */
    @GetMapping
    public List<PlaylistResponse> getPlaylists(
            @AuthenticationPrincipal UserDetails userDetails) {

        // UserDetails.getUsername()은 JwtAuthFilter에서 email로 설정했다
        return playlistService.getPlaylists(userDetails.getUsername());
    }

    /**
     * 새 플레이리스트를 생성한다.
     *
     * 요청 바디 예시:
     * {
     *   "name": "Lo-fi Chill"
     * }
     *
     * @param userDetails JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param request     플레이리스트 이름이 담긴 요청 DTO
     * @return 생성된 플레이리스트 DTO (HTTP 201 Created)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlaylistResponse createPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePlaylistRequest request) {

        return playlistService.createPlaylist(userDetails.getUsername(), request.name());
    }

    /**
     * 플레이리스트를 삭제한다.
     * Service에서 소유권 검증을 수행하므로, 다른 유저의 플레이리스트는 403으로 차단된다.
     *
     * @param userDetails  JWT 인증 필터가 SecurityContext에 저장한 유저 정보
     * @param playlistId   삭제할 플레이리스트 ID (URL 경로 변수)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 삭제 성공 시 응답 바디 없이 204 반환
    public void deletePlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long playlistId) {

        playlistService.deletePlaylist(userDetails.getUsername(), playlistId);
    }

    /**
     * 플레이리스트 생성 요청 DTO.
     *
     * record로 선언해 불변(immutable) 객체로 유지한다.
     * @NotBlank: null, 빈 문자열, 공백만 있는 문자열 모두 거부한다.
     */
    public record CreatePlaylistRequest(
            @NotBlank(message = "플레이리스트 이름은 필수입니다") String name
    ) {}
}
