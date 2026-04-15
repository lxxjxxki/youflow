package com.rawr.subscription;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock JavaMailSender mailSender;
    @InjectMocks SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    @DisplayName("새 이메일로 구독하면 저장된다")
    void subscribe_withNewEmail_savesSubscription() {
        when(subscriptionRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> subscriptionService.subscribe("new@test.com"))
                .doesNotThrowAnyException();
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("이미 구독 중인 이메일로 재구독 시 중복 저장하지 않는다")
    void subscribe_withExistingEmail_doesNotDuplicate() {
        when(subscriptionRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(new Subscription("existing@test.com")));

        subscriptionService.subscribe("existing@test.com");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("새 기사 발행 시 모든 구독자에게 이메일이 전송된다")
    void notifyNewArticle_sendsEmailToAllSubscribers() {
        User author = new User("a@t.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        Article article = new Article("Hot Drop", "hot-drop", "Content", null, Category.FASHION, author);
        Subscription sub1 = new Subscription("reader1@test.com");
        Subscription sub2 = new Subscription("reader2@test.com");

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub1, sub2));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        subscriptionService.notifyNewArticle(article);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("이메일에 기사 제목과 URL이 포함된다")
    void notifyNewArticle_emailContainsTitleAndUrl() {
        User author = new User("a@t.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        Article article = new Article("Amazing Fashion", "amazing-fashion", "Content", null, Category.FASHION, author);
        Subscription sub = new Subscription("reader@test.com");

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));

        subscriptionService.notifyNewArticle(article);

        verify(mailSender).send(argThat((SimpleMailMessage msg) -> {
            String text = msg.getText();
            return text != null
                    && text.contains("Amazing Fashion")
                    && text.contains("amazing-fashion")
                    && text.contains("unsubscribe");
        }));
    }

    @Test
    @DisplayName("구독자가 없을 때 이메일을 전송하지 않는다")
    void notifyNewArticle_noSubscribers_sendsNoEmail() {
        User author = new User("a@t.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        subscriptionService.notifyNewArticle(article);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("유효한 토큰으로 구독 취소하면 삭제된다")
    void unsubscribe_withValidToken_deletesSubscription() {
        Subscription sub = new Subscription("reader@test.com");
        when(subscriptionRepository.findByUnsubscribeToken("valid-token"))
                .thenReturn(Optional.of(sub));
        doNothing().when(subscriptionRepository).delete(any());

        subscriptionService.unsubscribe("valid-token");

        verify(subscriptionRepository).delete(sub);
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 구독 취소 시 아무 일도 일어나지 않는다")
    void unsubscribe_withInvalidToken_doesNothing() {
        when(subscriptionRepository.findByUnsubscribeToken("bad-token"))
                .thenReturn(Optional.empty());

        assertThatCode(() -> subscriptionService.unsubscribe("bad-token"))
                .doesNotThrowAnyException();
        verify(subscriptionRepository, never()).delete(any());
    }
}
