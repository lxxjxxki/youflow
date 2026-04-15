package com.rawr.bookmark;

import com.rawr.article.ArticleRepository;
import com.rawr.article.dto.ArticleResponse;
import com.rawr.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           ArticleRepository articleRepository,
                           UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public boolean toggle(UUID articleId, UUID userId) {
        return bookmarkRepository.findByArticleIdAndUserId(articleId, userId)
                .map(b -> { bookmarkRepository.delete(b); return false; })
                .orElseGet(() -> {
                    var article = articleRepository.findById(articleId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    bookmarkRepository.save(new Bookmark(article, user));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> myBookmarks(UUID userId) {
        return bookmarkRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(b -> ArticleResponse.from(b.getArticle()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(UUID articleId, UUID userId) {
        return bookmarkRepository.existsByArticleIdAndUserId(articleId, userId);
    }
}
