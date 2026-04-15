package com.rawr.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    private Role role = Role.READER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(nullable = false)
    private String oauthId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String email, String username, String profileImage,
                OAuthProvider oauthProvider, String oauthId) {
        this.email = email;
        this.username = username;
        this.profileImage = profileImage;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getProfileImage() { return profileImage; }
    public Role getRole() { return role; }
    public OAuthProvider getOauthProvider() { return oauthProvider; }
    public String getOauthId() { return oauthId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateProfile(String username, String profileImage) {
        this.username = username;
        this.profileImage = profileImage;
    }

    public void promoteToContributor() { this.role = Role.CONTRIBUTOR; }
}
