package com.youflow.video;

import com.youflow.playlist.Playlist;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 영상(Video) 엔티티 — videos 테이블과 1:1 매핑.
 *
 * 관계 설계: Video → Playlist 단방향 @ManyToOne.
 * Playlist 쪽에 @OneToMany를 두지 않아 불필요한 컬렉션 로딩을 방지한다.
 *
 * 핵심 필드:
 *   - youtubeId: iframe 임베드에 사용하는 YouTube 영상 고유 ID
 *   - position:  플레이리스트 내 재생 순서 (0-based)
 *   - volume:    이 영상에 적용할 음량 (0~100), 기본값 70
 */
@Entity
@Table(name = "videos")
public class Video {

    /** DB auto-increment PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 영상이 속한 플레이리스트.
     * LAZY 로딩: 영상 조회 시 Playlist 쿼리가 자동으로 나가지 않는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    /** 유저가 입력한 원본 YouTube URL */
    @Column(name = "youtube_url", nullable = false)
    private String youtubeUrl;

    /**
     * YouTube 영상 고유 ID (URL에서 파싱).
     * iframe 임베드, YouTube Data API 호출 모두 이 값을 사용한다.
     * 예: https://www.youtube.com/watch?v=dQw4w9WgXcQ → dQw4w9WgXcQ
     */
    @Column(name = "youtube_id", nullable = false)
    private String youtubeId;

    /** YouTube Data API로 가져온 영상 제목 */
    @Column(nullable = false)
    private String title;

    /** YouTube Data API로 가져온 썸네일 URL */
    @Column(nullable = false)
    private String thumbnail;

    /**
     * 플레이리스트 내 재생 순서 (0-based).
     * 영상 추가 시 Service에서 현재 최대 position + 1로 설정한다.
     */
    @Column(nullable = false)
    private int position = 0;

    /**
     * 이 영상에 적용할 음량 (0~100).
     * 기본값 70. 유저가 조절하면 실시간으로 DB에 반영되고,
     * 다음 재생 시 iframe의 onReady에서 player.setVolume()으로 적용된다.
     */
    @Column(nullable = false)
    private int volume = 70;

    /** 영상 추가 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** JPA 전용 기본 생성자 — 외부에서 직접 호출하지 않는다. */
    protected Video() {}

    /**
     * 영상 생성자. Service에서 YouTube API 응답을 파싱한 뒤 호출한다.
     *
     * @param playlist   영상이 추가될 플레이리스트
     * @param youtubeUrl 유저가 입력한 원본 URL
     * @param youtubeId  URL에서 파싱한 YouTube 영상 ID
     * @param title      YouTube API에서 가져온 영상 제목
     * @param thumbnail  YouTube API에서 가져온 썸네일 URL
     * @param position   플레이리스트 내 순서
     */
    public Video(Playlist playlist, String youtubeUrl, String youtubeId,
                 String title, String thumbnail, int position) {
        this.playlist = playlist;
        this.youtubeUrl = youtubeUrl;
        this.youtubeId = youtubeId;
        this.title = title;
        this.thumbnail = thumbnail;
        this.position = position;
    }

    public Long getId() { return id; }
    public Playlist getPlaylist() { return playlist; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getYoutubeId() { return youtubeId; }
    public String getTitle() { return title; }
    public String getThumbnail() { return thumbnail; }
    public int getPosition() { return position; }
    public int getVolume() { return volume; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** 음량 업데이트. WebSocket 또는 REST API로 유저가 조절할 때 호출한다. */
    public void updateVolume(int volume) {
        this.volume = volume;
    }

    /** 재생 순서 업데이트. 드래그-앤-드롭 재정렬 시 호출한다. */
    public void updatePosition(int position) {
        this.position = position;
    }
}
