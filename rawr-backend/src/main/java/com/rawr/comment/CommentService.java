package com.rawr.comment;

import com.rawr.article.ArticleRepository;
import com.rawr.comment.dto.CommentRequest;
import com.rawr.comment.dto.CommentResponse;
import com.rawr.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                          ArticleRepository articleRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public CommentResponse add(UUID articleId, UUID userId, CommentRequest request) {
        var article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return CommentResponse.from(commentRepository.save(new Comment(request.content(), article, user)));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId)
                .stream().map(CommentResponse::from).toList();
    }

    public void delete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your comment");
        }
        commentRepository.delete(comment);
    }
}
