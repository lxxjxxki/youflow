package com.rawr.comment.dto;

import com.rawr.comment.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        String authorName,
        UUID authorId,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getContent(),
                c.getUser().getUsername(), c.getUser().getId(), c.getCreatedAt());
    }
}
