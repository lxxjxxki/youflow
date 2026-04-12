package com.youflow.playlist;

import com.youflow.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 플레이리스트 엔티티 — playlists 테이블과 1:1 매핑.
 *
 * 관계 설계: Playlist → User 단방향 @ManyToOne.
 * User 쪽에 @OneToMany를 두지 않아 N+1 문제와 불필요한 컬렉션 로딩을 방지한다.
 */
@Entity
@Table(name = "playlists")
public class Playlist {

    /** DB auto-increment PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 플레이리스트를 소유한 유저.
     * LAZY 로딩: 플레이리스트를 조회할 때 User 쿼리가 자동으로 나가지 않는다.
     * user_id 컬럼으로 FK 참조.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 플레이리스트 이름 (빈 문자열 불허) */
    @Column(nullable = false)
    private String name;

    /** 생성 시각 — 객체 생성 시점에 자동 설정, 이후 변경하지 않는다. */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** JPA 전용 기본 생성자 — 외부에서 직접 호출하지 않는다. */
    protected Playlist() {}

    /**
     * 실제로 사용하는 생성자.
     *
     * @param user 플레이리스트 소유자
     * @param name 플레이리스트 이름
     */
    public Playlist(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
