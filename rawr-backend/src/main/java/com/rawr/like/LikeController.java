package com.rawr.like;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles/{articleId}/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public Map<String, Object> status(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        return likeService.status(articleId, userId);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        boolean liked = likeService.toggle(articleId, userId);
        return ResponseEntity.ok(Map.of(
                "liked", liked,
                "count", likeService.status(articleId, userId).get("count")
        ));
    }
}
