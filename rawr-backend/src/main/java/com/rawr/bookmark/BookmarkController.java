package com.rawr.bookmark;

import com.rawr.article.dto.ArticleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/api/articles/{articleId}/bookmarks")
    public ResponseEntity<Map<String, Boolean>> toggle(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        boolean bookmarked = bookmarkService.toggle(articleId, userId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @GetMapping("/api/users/me/bookmarks")
    public List<ArticleResponse> myBookmarks(@AuthenticationPrincipal UUID userId) {
        return bookmarkService.myBookmarks(userId);
    }
}
