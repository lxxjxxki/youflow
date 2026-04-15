package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.article.dto.ArticleResponse;
import com.rawr.subscription.SubscriptionService;
import com.rawr.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.UUID;

@Service
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public ArticleService(ArticleRepository articleRepository,
                          UserRepository userRepository,
                          SubscriptionService subscriptionService) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    public ArticleResponse create(UUID authorId, ArticleRequest request) {
        var author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String slug = uniqueSlug(toSlug(request.title()));
        Article article = new Article(request.title(), slug, request.content(),
                request.coverImage(), request.category(), author);
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional(readOnly = true)
    public Page<ArticleResponse> listPublished(Category category, Pageable pageable) {
        Page<Article> page = category != null
                ? articleRepository.findByStatusAndCategory(ArticleStatus.PUBLISHED, category, pageable)
                : articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable);
        return page.map(ArticleResponse::from);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getBySlug(String slug) {
        return articleRepository.findBySlug(slug)
                .map(ArticleResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    public ArticleResponse update(UUID articleId, UUID userId, ArticleRequest request) {
        Article article = findArticleOwnedBy(articleId, userId);
        String slug = article.getTitle().equals(request.title())
                ? article.getSlug()
                : uniqueSlug(toSlug(request.title()));
        article.update(request.title(), slug, request.content(), request.coverImage(), request.category());
        return ArticleResponse.from(articleRepository.save(article));
    }

    public ArticleResponse publish(UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        article.publish();
        Article saved = articleRepository.save(article);
        subscriptionService.notifyNewArticle(saved);
        return ArticleResponse.from(saved);
    }

    public ArticleResponse unpublish(UUID articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        article.unpublish();
        return ArticleResponse.from(articleRepository.save(article));
    }

    public void delete(UUID articleId, UUID userId) {
        Article article = findArticleOwnedBy(articleId, userId);
        articleRepository.delete(article);
    }

    private Article findArticleOwnedBy(UUID articleId, UUID userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        if (!article.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your article");
        }
        return article;
    }

    private String toSlug(String title) {
        return Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }

    private String uniqueSlug(String base) {
        if (!articleRepository.existsBySlug(base)) return base;
        int i = 2;
        while (articleRepository.existsBySlug(base + "-" + i)) i++;
        return base + "-" + i;
    }
}
