package com.rawr.comment;

import com.rawr.article.*;
import com.rawr.comment.dto.CommentRequest;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CommentService commentService;

    User author;
    User commenter;
    Article article;

    @BeforeEach
    void setUp() {
        author = new User("author@test.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        ReflectionTestUtils.setField(author, "id", UUID.randomUUID());
        commenter = new User("bob@test.com", "Bob", null, OAuthProvider.GOOGLE, "g2");
        ReflectionTestUtils.setField(commenter, "id", UUID.randomUUID());
        article = new Article("Title", "title", "Content", null, Category.FASHION, author);
    }

    @Test
    @DisplayName("댓글을 추가하면 저장되고 작성자 정보가 포함된다")
    void addComment_savesComment() {
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(commenter));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = commentService.add(UUID.randomUUID(), UUID.randomUUID(),
                new CommentRequest("Great article!"));

        assertThat(response.content()).isEqualTo("Great article!");
        assertThat(response.authorName()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("기사의 댓글 목록을 최신순으로 조회한다")
    void listComments_returnsInDescendingOrder() {
        Comment c1 = new Comment("First", article, commenter);
        Comment c2 = new Comment("Second", article, author);
        when(commentRepository.findByArticleIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(c2, c1));

        var result = commentService.list(UUID.randomUUID());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("Second");
        assertThat(result.get(1).content()).isEqualTo("First");
    }

    @Test
    @DisplayName("댓글 작성자가 본인 댓글을 삭제할 수 있다")
    void deleteComment_byOwner_succeeds() {
        Comment comment = new Comment("text", article, commenter);
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(any());

        assertThatCode(() -> commentService.delete(UUID.randomUUID(), commenter.getId()))
                .doesNotThrowAnyException();
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("다른 사람의 댓글을 삭제하려 하면 403 예외가 발생한다")
    void deleteComment_byNonOwner_throwsForbidden() {
        Comment comment = new Comment("text", article, author);
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(UUID.randomUUID(), commenter.getId()))
                .hasMessageContaining("Not your comment");
    }

    @Test
    @DisplayName("존재하지 않는 기사에 댓글 추가 시 404 예외가 발생한다")
    void addComment_nonExistentArticle_throwsNotFound() {
        when(articleRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            commentService.add(UUID.randomUUID(), UUID.randomUUID(), new CommentRequest("text"))
        ).hasMessageContaining("Article not found");
    }
}
