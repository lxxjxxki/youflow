package com.rawr.comment;

import com.rawr.comment.dto.CommentRequest;
import com.rawr.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID articleId) {
        return commentService.list(articleId);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> add(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(articleId, userId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID articleId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
