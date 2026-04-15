package com.rawr.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(@NotBlank String content) {}
