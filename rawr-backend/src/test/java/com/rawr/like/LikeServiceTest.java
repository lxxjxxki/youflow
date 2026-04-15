package com.rawr.like;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks LikeService likeService;

    User user;
    Article article;

    @BeforeEach
    void setUp() {
        user = new User("user@test.com", "User", null, OAuthProvider.GOOGLE, "g1");
        article = new Article("Title", "title", "Content", null, Category.FASHION, user);
    }

    @Test
    @DisplayName("좋아요가 없을 때 toggle하면 좋아요가 추가되고 true를 반환한다")
    void toggle_whenNotLiked_createsLike() {
        when(likeRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(likeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean liked = likeService.toggle(UUID.randomUUID(), UUID.randomUUID());

        assertThat(liked).isTrue();
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    @DisplayName("이미 좋아요가 있을 때 toggle하면 좋아요가 삭제되고 false를 반환한다")
    void toggle_whenAlreadyLiked_removesLike() {
        Like existing = new Like(article, user);
        when(likeRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.of(existing));
        doNothing().when(likeRepository).delete(any());

        boolean liked = likeService.toggle(UUID.randomUUID(), UUID.randomUUID());

        assertThat(liked).isFalse();
        verify(likeRepository).delete(existing);
        verify(likeRepository, never()).save(any());
    }

    @Test
    @DisplayName("좋아요 수와 현재 유저의 좋아요 여부를 함께 반환한다")
    void status_returnsCountAndLikedFlag() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(likeRepository.countByArticleId(articleId)).thenReturn(5L);
        when(likeRepository.existsByArticleIdAndUserId(articleId, userId)).thenReturn(true);

        var result = likeService.status(articleId, userId);

        assertThat(result.get("count")).isEqualTo(5L);
        assertThat(result.get("liked")).isEqualTo(true);
    }

    @Test
    @DisplayName("비로그인 유저는 liked가 항상 false다")
    void status_withNullUserId_likedIsFalse() {
        UUID articleId = UUID.randomUUID();
        when(likeRepository.countByArticleId(articleId)).thenReturn(3L);

        var result = likeService.status(articleId, null);

        assertThat(result.get("liked")).isEqualTo(false);
        verify(likeRepository, never()).existsByArticleIdAndUserId(any(), any());
    }
}
