package com.rawr.article.dto;

import com.rawr.article.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRequest(
        @NotBlank String title,
        @NotBlank String content,
        String coverImage,
        @NotNull Category category
) {}
