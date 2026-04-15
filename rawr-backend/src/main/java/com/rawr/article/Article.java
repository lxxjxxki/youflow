package com.rawr.article;

import com.rawr.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "article_category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "article_status")
    private ArticleStatus status = ArticleStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Article() {}

    public Article(String title, String slug, String content,
                   String coverImage, Category category, User author) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.author = author;
    }

    public void update(String title, String slug, String content,
                       String coverImage, Category category) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.status = ArticleStatus.DRAFT;
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getContent() { return content; }
    public String getCoverImage() { return coverImage; }
    public Category getCategory() { return category; }
    public ArticleStatus getStatus() { return status; }
    public User getAuthor() { return author; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
