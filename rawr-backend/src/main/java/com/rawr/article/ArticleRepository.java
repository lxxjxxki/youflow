package com.rawr.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Optional<Article> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    Page<Article> findByStatusAndCategory(ArticleStatus status, Category category, Pageable pageable);
    List<Article> findByStatus(ArticleStatus status);
}
