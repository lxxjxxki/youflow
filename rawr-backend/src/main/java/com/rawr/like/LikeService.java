package com.rawr.like;

import com.rawr.article.ArticleRepository;
import com.rawr.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public LikeService(LikeRepository likeRepository,
                       ArticleRepository articleRepository,
                       UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public boolean toggle(UUID articleId, UUID userId) {
        return likeRepository.findByArticleIdAndUserId(articleId, userId)
                .map(like -> { likeRepository.delete(like); return false; })
                .orElseGet(() -> {
                    var article = articleRepository.findById(articleId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    likeRepository.save(new Like(article, user));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(UUID articleId, UUID userId) {
        return Map.of(
                "count", likeRepository.countByArticleId(articleId),
                "liked", userId != null && likeRepository.existsByArticleIdAndUserId(articleId, userId)
        );
    }
}
