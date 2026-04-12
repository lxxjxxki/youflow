package com.youflow.playlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 플레이리스트 데이터 접근 계층.
 *
 * Spring Data JPA가 인터페이스 선언만으로 구현체를 자동 생성한다.
 * 기본 CRUD(save, findById, delete 등)는 JpaRepository에서 제공되므로 별도 선언 불필요.
 */
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    /**
     * 특정 유저의 플레이리스트를 최신순(createdAt DESC)으로 조회한다.
     *
     * 메서드 이름 규칙 분석:
     *   findBy          → SELECT ... WHERE
     *   UserEmail       → JOIN user ON playlist.user_id = user.id WHERE user.email = ?
     *   OrderByCreatedAtDesc → ORDER BY playlist.created_at DESC
     *
     * @param email 조회할 유저의 이메일
     * @return 해당 유저의 플레이리스트 목록 (없으면 빈 리스트)
     */
    List<Playlist> findByUserEmailOrderByCreatedAtDesc(String email);
}
