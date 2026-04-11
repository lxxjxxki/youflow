package com.youflow.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
/** 토큰 생성/검증 */
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
        @Value("${youflow.jwt.secret}") String secret,
        @Value("${youflow.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        // secret 문자열을 HMAC-SHA 키로 변환
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        // subject에 email을 담고 서명해서 JWT 문자열 반환
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        // 파싱 시 예외 발생하면 false, 정상이면 true
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
//        토큰 payload의 subject(email) 추출
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
}
