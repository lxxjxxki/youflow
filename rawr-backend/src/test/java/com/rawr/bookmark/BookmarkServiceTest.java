package com.rawr.bookmark;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock BookmarkRepository bookmarkRepository;
    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks BookmarkService bookmarkService;

    User user;
    Article article;

    @BeforeEach
    void setUp() {
        user = new User("user@test.com", "User", null, OAuthProvider.GOOGLE, "g1");
        article = new Article("Title", "title", "Content", null, Category.FASHION, user);
    }

    @Test
    @DisplayName("북마크가 없을 때 toggle하면 북마크가 추가되고 true를 반환한다")
    void toggle_whenNotBookmarked_createsBookmark() {
        when(bookmarkRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(bookmarkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean bookmarked = bookmarkService.toggle(UUID.randomUUID(), UUID.randomUUID());

        assertThat(bookmarked).isTrue();
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("이미 북마크가 있을 때 toggle하면 북마크가 삭제되고 false를 반환한다")
    void toggle_whenAlreadyBookmarked_removesBookmark() {
        Bookmark existing = new Bookmark(article, user);
        when(bookmarkRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.of(existing));
        doNothing().when(bookmarkRepository).delete(any());

        boolean bookmarked = bookmarkService.toggle(UUID.randomUUID(), UUID.randomUUID());

        assertThat(bookmarked).isFalse();
        verify(bookmarkRepository).delete(existing);
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("내 북마크 목록을 조회하면 북마크한 기사 목록이 반환된다")
    void myBookmarks_returnsBookmarkedArticles() {
        Article article2 = new Article("Title2", "title-2", "Content2", null, Category.CULTURE, user);
        Bookmark b1 = new Bookmark(article, user);
        Bookmark b2 = new Bookmark(article2, user);
        when(bookmarkRepository.findByUserIdOrderByIdDesc(any())).thenReturn(List.of(b2, b1));

        var result = bookmarkService.myBookmarks(UUID.randomUUID());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Title2");
        assertThat(result.get(1).title()).isEqualTo("Title");
    }

    @Test
    @DisplayName("북마크 여부를 확인할 수 있다")
    void isBookmarked_returnsCorrectFlag() {
        UUID articleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(bookmarkRepository.existsByArticleIdAndUserId(articleId, userId)).thenReturn(true);

        assertThat(bookmarkService.isBookmarked(articleId, userId)).isTrue();
    }
}
