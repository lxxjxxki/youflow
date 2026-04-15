package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.subscription.SubscriptionService;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionService subscriptionService;
    @InjectMocks ArticleService articleService;

    User author;

    @BeforeEach
    void setUp() {
        author = new User("author@test.com", "Author", null, OAuthProvider.GOOGLE, "google-123");
        ReflectionTestUtils.setField(author, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("기사 생성 시 DRAFT 상태로 저장된다")
    void createArticle_savesAsDraft() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new ArticleRequest("My Title", "Content here", "cover.jpg", Category.FASHION);
        var response = articleService.create(UUID.randomUUID(), request);

        assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(response.slug()).isEqualTo("my-title");
        assertThat(response.title()).isEqualTo("My Title");
        assertThat(response.category()).isEqualTo(Category.FASHION);
    }

    @Test
    @DisplayName("기사 발행 시 PUBLISHED 상태로 변경되고 구독자에게 알림이 전송된다")
    void publishArticle_setsPublishedStatusAndNotifiesSubscribers() {
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(subscriptionService).notifyNewArticle(any());

        var response = articleService.publish(article.getId());

        assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
        verify(subscriptionService, times(1)).notifyNewArticle(any());
    }

    @Test
    @DisplayName("발행된 기사를 다시 DRAFT로 되돌릴 수 있다")
    void unpublishArticle_revertsToDraft() {
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        article.publish();
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = articleService.unpublish(article.getId());

        assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(response.publishedAt()).isNull();
    }

    @Test
    @DisplayName("슬러그 중복 시 숫자를 붙여 유니크하게 만든다")
    void createArticle_duplicateSlug_appendsNumber() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug("my-title")).thenReturn(true);
        when(articleRepository.existsBySlug("my-title-2")).thenReturn(false);
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new ArticleRequest("My Title", "Content", null, Category.CULTURE);
        var response = articleService.create(UUID.randomUUID(), request);

        assertThat(response.slug()).isEqualTo("my-title-2");
    }

    @Test
    @DisplayName("슬러그가 연속으로 중복될 경우 빈 번호를 찾아 붙인다")
    void createArticle_multipleDuplicateSlugs_appendsCorrectNumber() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug("my-title")).thenReturn(true);
        when(articleRepository.existsBySlug("my-title-2")).thenReturn(true);
        when(articleRepository.existsBySlug("my-title-3")).thenReturn(false);
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = articleService.create(UUID.randomUUID(), new ArticleRequest("My Title", "C", null, Category.FASHION));

        assertThat(response.slug()).isEqualTo("my-title-3");
    }

    @Test
    @DisplayName("한글 제목은 슬러그 생성 시 ASCII 문자만 남긴다")
    void createArticle_koreanTitle_stripsNonAscii() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug(anyString())).thenReturn(false);
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = articleService.create(UUID.randomUUID(),
                new ArticleRequest("패션 트렌드 2026", "Content", null, Category.FASHION));

        // Korean characters stripped, only remaining ASCII kept
        assertThat(response.slug()).doesNotContain("패").doesNotContain("션");
    }

    @Test
    @DisplayName("본인 기사만 수정할 수 있다")
    void updateArticle_byNonOwner_throwsForbidden() {
        UUID otherUserId = UUID.randomUUID();
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));

        assertThatThrownBy(() ->
            articleService.update(article.getId(), otherUserId,
                new ArticleRequest("New Title", "New Content", null, Category.CULTURE))
        ).hasMessageContaining("Not your article");
    }

    @Test
    @DisplayName("존재하지 않는 기사 조회 시 404 예외가 발생한다")
    void getBySlug_nonExistent_throwsNotFound() {
        when(articleRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getBySlug("non-existent"))
                .hasMessageContaining("Article not found");
    }

    @Test
    @DisplayName("본인 기사를 삭제할 수 있다")
    void deleteArticle_byOwner_succeeds() {
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        ReflectionTestUtils.setField(article, "id", UUID.randomUUID());
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        doNothing().when(articleRepository).delete(any());

        assertThatCode(() -> articleService.delete(article.getId(), author.getId()))
                .doesNotThrowAnyException();
        verify(articleRepository).delete(article);
    }
}
