package com.rawr.subscription;

import com.rawr.article.Article;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final JavaMailSender mailSender;

    @Value("${rawr.frontend-url}")
    private String frontendUrl;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               JavaMailSender mailSender) {
        this.subscriptionRepository = subscriptionRepository;
        this.mailSender = mailSender;
    }

    public void subscribe(String email) {
        if (subscriptionRepository.findByEmail(email).isEmpty()) {
            subscriptionRepository.save(new Subscription(email));
        }
    }

    public void unsubscribe(String token) {
        subscriptionRepository.findByUnsubscribeToken(token)
                .ifPresent(subscriptionRepository::delete);
    }

    public void notifyNewArticle(Article article) {
        String subject = "New on rawr.co.kr: " + article.getTitle();
        subscriptionRepository.findAll().forEach(sub -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(sub.getEmail());
            message.setSubject(subject);
            message.setText(
                "A new article has been published on rawr.co.kr.\n\n" +
                article.getTitle() + "\n" +
                frontendUrl + "/articles/" + article.getSlug() + "\n\n" +
                "Unsubscribe: " + frontendUrl + "/unsubscribe?token=" + sub.getUnsubscribeToken()
            );
            mailSender.send(message);
        });
    }
}
