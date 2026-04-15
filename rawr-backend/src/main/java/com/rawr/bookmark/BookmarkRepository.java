package com.rawr.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByArticleIdAndUserId(UUID articleId, UUID userId);
    List<Bookmark> findByUserIdOrderByIdDesc(UUID userId);
    boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
}
