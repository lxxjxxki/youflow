package com.youflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket(STOMP) 설정.
 *
 * 메시지 흐름:
 *   클라이언트 → /app/room/{email}/command → WebSocketController 처리
 *                                           → /topic/room/{email} 브로드캐스트
 *                                           → 같은 룸을 구독한 모든 클라이언트 수신
 *
 * SockJS fallback:
 *   WebSocket을 지원하지 않는 환경(일부 기업 프록시 등)에서
 *   롱폴링 등 대안 프로토콜로 자동 전환된다.
 *   스마트폰 등 다양한 기기 지원에 필수적이다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 클라이언트가 WebSocket 연결을 맺는 엔드포인트.
     * SecurityConfig에서 /ws/** 를 permitAll()로 열어둔 상태다.
     *
     * 프론트엔드 연결 예시:
     *   const socket = new SockJS('/ws');
     *   const client = new Client({ webSocketFactory: () => socket });
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp")
                .setAllowedOriginPatterns("*") // 개발 중 CORS 허용, 배포 시 도메인 지정 필요
                .withSockJS()
                .setSessionCookieNeeded(false); // Vite 프록시 환경에서 쿠키 불필요
    }

    /**
     * 메시지 브로커 설정.
     *
     * - /app : 클라이언트 → 서버 메시지 prefix (WebSocketController의 @MessageMapping과 매핑)
     * - /topic : 서버 → 클라이언트 브로드캐스트 prefix (구독 채널)
     *
     * 현재는 인메모리 브로커 사용.
     * 향후 다중 서버 확장 시 RabbitMQ/Redis 브로커로 교체 가능.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }
}
