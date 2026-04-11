package com.youflow.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
/** Playlist 연관관계 → User에 안 걸고 Playlist에서 @ManyToOne만 갖는 단방향 */
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    /** Java에서 LocalDateTime.now()로 직접 세팅. DB DEFAULT NOW()를 믿지 않고 애플리케이션이 제어 */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String email, String passwordHash) {
        // JPA 스펙상 기본 생성자 필요, protected로 외부 직접 생성 막음
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
