package com.youflow.playlist;

import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 플레이리스트 비즈니스 로직 담당 서비스.
 *
 * 역할:
 *   - Controller로부터 요청을 받아 유효성·소유권 검증 후 Repository에 위임
 *   - Entity를 직접 반환하지 않고 PlaylistResponse DTO로 변환하여 반환
 *
 * 트랜잭션 전략:
 *   - 클래스 레벨: @Transactional(readOnly = true)
 *     → 조회 전용 트랜잭션. DB가 flush를 생략해 성능상 유리하다.
 *   - 변경 메서드: @Transactional (readOnly 없음)
 *     → 변경 감지(dirty checking), flush, 롤백이 모두 활성화된다.
 */
@Service
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    /** 생성자 주입 — Spring이 두 Repository 구현체를 자동으로 주입한다. */
    public PlaylistService(PlaylistRepository playlistRepository, UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.userRepository = userRepository;
    }

    /**
     * 현재 로그인한 유저의 플레이리스트 목록을 최신순으로 반환한다.
     *
     * @param email JWT에서 추출한 로그인 유저의 이메일
     * @return 플레이리스트 DTO 목록 (플레이리스트가 없으면 빈 리스트)
     */
    public List<PlaylistResponse> getPlaylists(String email) {
        // Repository에서 가져온 Entity 리스트를 Stream으로 DTO 변환
        return playlistRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(PlaylistResponse::from)
                .toList();
    }

    /**
     * 새 플레이리스트를 생성하고 저장한다.
     *
     * 흐름:
     *   1. email로 유저 조회 (없으면 404)
     *   2. Playlist 엔티티 생성
     *   3. 저장 후 DTO 반환
     *
     * @param email 플레이리스트를 생성할 유저의 이메일
     * @param name  플레이리스트 이름
     * @return 생성된 플레이리스트의 DTO
     * @throws ResponseStatusException 404 — 해당 email의 유저가 DB에 없을 때
     */
    @Transactional
    public PlaylistResponse createPlaylist(String email, String name) {
        // 유저 조회: JWT 인증을 통과했더라도 DB에서 한 번 더 확인한다.
        // (계정이 삭제된 상태에서 만료 전 토큰으로 요청하는 케이스 대비)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Playlist playlist = new Playlist(user, name);
        Playlist saved = playlistRepository.save(playlist);
        return PlaylistResponse.from(saved);
    }

    /**
     * 플레이리스트를 삭제한다.
     *
     * 보안 핵심: 플레이리스트 ID만 알고 있어도 다른 유저가 삭제를 시도할 수 있다.
     * 따라서 소유자 이메일 일치 여부를 반드시 검증한다.
     *
     * 흐름:
     *   1. playlistId로 플레이리스트 조회 (없으면 404)
     *   2. 요청자 email과 소유자 email 비교 (불일치 시 403)
     *   3. 삭제
     *
     * @param email      JWT에서 추출한 요청자 이메일
     * @param playlistId 삭제할 플레이리스트의 ID
     * @throws ResponseStatusException 404 — 플레이리스트가 존재하지 않을 때
     * @throws ResponseStatusException 403 — 요청자가 소유자가 아닐 때
     */
    @Transactional
    public void deletePlaylist(String email, Long playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));

        // 소유권 검증: 다른 유저의 플레이리스트 삭제 시도 차단
        if (!playlist.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your playlist");
        }

        playlistRepository.delete(playlist);
    }
}
