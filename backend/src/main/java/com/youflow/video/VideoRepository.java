package com.youflow.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 영상 데이터 접근 계층.
 *
 * Spring Data JPA가 인터페이스 선언만으로 구현체를 자동 생성한다.
 */
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * 특정 플레이리스트의 영상 목록을 재생 순서(position ASC)로 조회한다.
     *
     * @param playlistId 조회할 플레이리스트 ID
     * @return 순서대로 정렬된 영상 목록 (없으면 빈 리스트)
     */
    List<Video> findByPlaylistIdOrderByPositionAsc(Long playlistId);

    /**
     * 특정 플레이리스트에서 현재 가장 큰 position 값을 조회한다.
     *
     * 새 영상 추가 시 Service에서 이 값 + 1을 새 영상의 position으로 설정한다.
     * 플레이리스트에 영상이 없으면 null을 반환하므로 Service에서 null 처리 필요.
     *
     * @param playlistId 조회할 플레이리스트 ID
     * @return 현재 최대 position 값, 영상이 없으면 null
     */
    @Query("SELECT MAX(v.position) FROM Video v WHERE v.playlist.id = :playlistId")
    Integer findMaxPositionByPlaylistId(@Param("playlistId") Long playlistId);
}
