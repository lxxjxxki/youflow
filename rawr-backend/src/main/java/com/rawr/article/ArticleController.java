package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.article.dto.ArticleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Page<ArticleResponse> list(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return articleService.listPublished(category,
                PageRequest.of(page, size, Sort.by("publishedAt").descending()));
    }

    @GetMapping("/{slug}")
    public ArticleResponse get(@PathVariable String slug) {
        return articleService.getBySlug(slug);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ResponseEntity<ArticleResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.create(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ArticleResponse update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ArticleRequest request) {
        return articleService.update(id, userId, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER')")
    public ArticleResponse publish(@PathVariable UUID id) {
        return articleService.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('OWNER')")
    public ArticleResponse unpublish(@PathVariable UUID id) {
        return articleService.unpublish(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        articleService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
