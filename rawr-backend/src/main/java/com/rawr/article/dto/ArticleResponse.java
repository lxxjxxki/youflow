package com.rawr.article.dto;

import com.rawr.article.Article;
import com.rawr.article.ArticleStatus;
import com.rawr.article.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String title,
        String slug,
        String content,
        String coverImage,
        Category category,
        ArticleStatus status,
        String authorName,
        UUID authorId,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArticleResponse from(Article a) {
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getSlug(),
                a.getContent(), a.getCoverImage(), a.getCategory(),
                a.getStatus(), a.getAuthor().getUsername(), a.getAuthor().getId(),
                a.getPublishedAt(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
