package com.rawr.like;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByArticleIdAndUserId(UUID articleId, UUID userId);
    long countByArticleId(UUID articleId);
    boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
}
