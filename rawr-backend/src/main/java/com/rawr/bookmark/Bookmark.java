package com.rawr.bookmark;

import com.rawr.article.Article;
import com.rawr.user.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Bookmark() {}

    public Bookmark(Article article, User user) {
        this.article = article;
        this.user = user;
    }

    public UUID getId() { return id; }
    public Article getArticle() { return article; }
    public User getUser() { return user; }
}
