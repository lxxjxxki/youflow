# rawr.co.kr Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Spring Boot REST API for rawr.co.kr — a personal editorial magazine covering fashion and culture, with OAuth2 social login, article management, comments, likes, bookmarks, and email subscriptions.

**Architecture:** New Spring Boot 3.4 project at `rawr-backend/` in the monorepo. Follows the same patterns as the sibling `backend/` (youflow) project — domain-per-package, JPA entities, Flyway migrations, JWT auth — but replaces email/password auth with OAuth2 (Kakao, Google, Apple). Image uploads go to AWS S3.

**Tech Stack:** Java 21, Spring Boot 3.4, Spring Security OAuth2 Client, PostgreSQL, Flyway, JWT (jjwt 0.12.5), AWS SDK S3, Spring Mail, JUnit 5 + Mockito

---

## File Map

```
rawr-backend/
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/com/rawr/
│   │   │   ├── RawrApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── S3Config.java
│   │   │   ├── auth/
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── OAuth2UserService.java
│   │   │   │   └── dto/OAuthAttributes.java
│   │   │   ├── user/
│   │   │   │   ├── User.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── Role.java
│   │   │   │   └── OAuthProvider.java
│   │   │   ├── article/
│   │   │   │   ├── Article.java
│   │   │   │   ├── ArticleRepository.java
│   │   │   │   ├── ArticleService.java
│   │   │   │   ├── ArticleController.java
│   │   │   │   ├── Category.java
│   │   │   │   ├── ArticleStatus.java
│   │   │   │   └── dto/
│   │   │   │       ├── ArticleRequest.java
│   │   │   │       └── ArticleResponse.java
│   │   │   ├── comment/
│   │   │   │   ├── Comment.java
│   │   │   │   ├── CommentRepository.java
│   │   │   │   ├── CommentService.java
│   │   │   │   ├── CommentController.java
│   │   │   │   └── dto/
│   │   │   │       ├── CommentRequest.java
│   │   │   │       └── CommentResponse.java
│   │   │   ├── like/
│   │   │   │   ├── Like.java
│   │   │   │   ├── LikeRepository.java
│   │   │   │   ├── LikeService.java
│   │   │   │   └── LikeController.java
│   │   │   ├── bookmark/
│   │   │   │   ├── Bookmark.java
│   │   │   │   ├── BookmarkRepository.java
│   │   │   │   ├── BookmarkService.java
│   │   │   │   └── BookmarkController.java
│   │   │   ├── subscription/
│   │   │   │   ├── Subscription.java
│   │   │   │   ├── SubscriptionRepository.java
│   │   │   │   ├── SubscriptionService.java
│   │   │   │   └── SubscriptionController.java
│   │   │   └── image/
│   │   │       ├── ImageService.java
│   │   │       └── ImageController.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           └── V1__init.sql
│   └── test/
│       ├── java/com/rawr/
│       │   ├── article/ArticleServiceTest.java
│       │   ├── article/ArticleControllerTest.java
│       │   ├── comment/CommentServiceTest.java
│       │   ├── comment/CommentControllerTest.java
│       │   ├── like/LikeServiceTest.java
│       │   ├── bookmark/BookmarkServiceTest.java
│       │   └── subscription/SubscriptionServiceTest.java
│       └── resources/application-test.yml
```

---

## Task 1: Project Scaffold

**Files:**
- Create: `rawr-backend/build.gradle`
- Create: `rawr-backend/src/main/java/com/rawr/RawrApplication.java`
- Create: `rawr-backend/src/main/resources/application.yml`
- Create: `rawr-backend/src/test/resources/application-test.yml`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p rawr-backend/src/main/java/com/rawr
mkdir -p rawr-backend/src/main/resources/db/migration
mkdir -p rawr-backend/src/test/java/com/rawr
mkdir -p rawr-backend/src/test/resources
mkdir -p rawr-backend/gradle/wrapper
```

- [ ] **Step 2: Copy Gradle wrapper from sibling project**

```bash
cp -r backend/gradle rawr-backend/gradle
cp backend/gradlew rawr-backend/gradlew
cp backend/gradlew.bat rawr-backend/gradlew.bat
chmod +x rawr-backend/gradlew
```

- [ ] **Step 3: Create `rawr-backend/build.gradle`**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.4'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.rawr'
version = '0.0.1-SNAPSHOT'
java { sourceCompatibility = JavaVersion.VERSION_21 }

repositories { mavenCentral() }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    implementation 'software.amazon.awssdk:s3:2.25.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.named('test') {
    useJUnitPlatform()
    jvmArgs '-Dnet.bytebuddy.experimental=true'
}
```

- [ ] **Step 4: Create `rawr-backend/src/main/java/com/rawr/RawrApplication.java`**

```java
package com.rawr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RawrApplication {
    public static void main(String[] args) {
        SpringApplication.run(RawrApplication.class, args);
    }
}
```

- [ ] **Step 5: Create `rawr-backend/src/main/resources/application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rawr
    username: rawr
    password: rawr
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            scope: account_email, profile_nickname
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

rawr:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 86400000
  frontend-url: ${FRONTEND_URL:http://localhost:3000}
  s3:
    bucket: ${S3_BUCKET}
    region: ${S3_REGION:ap-northeast-2}

server:
  port: 8081
```

- [ ] **Step 6: Create `rawr-backend/src/test/resources/application-test.yml`**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  flyway:
    enabled: false
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test
            client-secret: test
          kakao:
            client-id: test
            client-secret: test
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost/login/oauth2/code/kakao
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

rawr:
  jwt:
    secret: test-secret-key-for-testing-only-minimum-256-bits
    expiration-ms: 86400000
  frontend-url: http://localhost:3000
  s3:
    bucket: test-bucket
    region: ap-northeast-2
```

> Note: Add `testRuntimeOnly 'com.h2database:h2'` to build.gradle dependencies for in-memory test DB.

- [ ] **Step 7: Update `rawr-backend/build.gradle` to add H2 test dependency**

Add inside `dependencies { }`:
```groovy
testRuntimeOnly 'com.h2database:h2'
```

- [ ] **Step 8: Verify project builds**

```bash
cd rawr-backend && ./gradlew build -x test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add rawr-backend/
git commit -m "feat(rawr): scaffold Spring Boot project"
```

---

## Task 2: Database Schema

**Files:**
- Create: `rawr-backend/src/main/resources/db/migration/V1__init.sql`

- [ ] **Step 1: Create `V1__init.sql`**

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('OWNER', 'CONTRIBUTOR', 'READER');
CREATE TYPE oauth_provider AS ENUM ('KAKAO', 'GOOGLE', 'APPLE');
CREATE TYPE article_category AS ENUM ('FASHION', 'CULTURE');
CREATE TYPE article_status AS ENUM ('DRAFT', 'PUBLISHED');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL,
    profile_image VARCHAR(500),
    role user_role NOT NULL DEFAULT 'READER',
    oauth_provider oauth_provider NOT NULL,
    oauth_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (oauth_provider, oauth_id)
);

CREATE TABLE articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    cover_image VARCHAR(500),
    category article_category NOT NULL,
    status article_status NOT NULL DEFAULT 'DRAFT',
    author_id UUID NOT NULL REFERENCES users(id),
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    UNIQUE (article_id, user_id)
);

CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    UNIQUE (article_id, user_id)
);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id),
    unsubscribe_token VARCHAR(64) NOT NULL UNIQUE DEFAULT encode(gen_random_bytes(32), 'hex'),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: Commit**

```bash
git add rawr-backend/src/main/resources/db/migration/V1__init.sql
git commit -m "feat(rawr): add database schema migration"
```

---

## Task 3: User Entity & Enums

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/user/Role.java`
- Create: `rawr-backend/src/main/java/com/rawr/user/OAuthProvider.java`
- Create: `rawr-backend/src/main/java/com/rawr/user/User.java`
- Create: `rawr-backend/src/main/java/com/rawr/user/UserRepository.java`

- [ ] **Step 1: Create `Role.java`**

```java
package com.rawr.user;

public enum Role {
    OWNER, CONTRIBUTOR, READER
}
```

- [ ] **Step 2: Create `OAuthProvider.java`**

```java
package com.rawr.user;

public enum OAuthProvider {
    KAKAO, GOOGLE, APPLE
}
```

- [ ] **Step 3: Create `User.java`**

```java
package com.rawr.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String username;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    private Role role = Role.READER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(nullable = false)
    private String oauthId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String email, String username, String profileImage,
                OAuthProvider oauthProvider, String oauthId) {
        this.email = email;
        this.username = username;
        this.profileImage = profileImage;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getProfileImage() { return profileImage; }
    public Role getRole() { return role; }
    public OAuthProvider getOauthProvider() { return oauthProvider; }
    public String getOauthId() { return oauthId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateProfile(String username, String profileImage) {
        this.username = username;
        this.profileImage = profileImage;
    }

    public void promoteToContributor() { this.role = Role.CONTRIBUTOR; }
}
```

- [ ] **Step 4: Create `UserRepository.java`**

```java
package com.rawr.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider provider, String oauthId);
    Optional<User> findByEmail(String email);
}
```

- [ ] **Step 5: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/user/
git commit -m "feat(rawr): add User entity and enums"
```

---

## Task 4: JWT Utility

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/auth/JwtUtil.java`

- [ ] **Step 1: Create `JwtUtil.java`**

```java
package com.rawr.auth;

import com.rawr.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${rawr.jwt.secret}") String secret,
            @Value("${rawr.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/auth/JwtUtil.java
git commit -m "feat(rawr): add JWT utility"
```

---

## Task 5: OAuth2 Auth

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/auth/dto/OAuthAttributes.java`
- Create: `rawr-backend/src/main/java/com/rawr/auth/OAuth2UserService.java`
- Create: `rawr-backend/src/main/java/com/rawr/auth/AuthController.java`
- Create: `rawr-backend/src/main/java/com/rawr/config/JwtAuthFilter.java`
- Create: `rawr-backend/src/main/java/com/rawr/config/SecurityConfig.java`
- Create: `rawr-backend/src/main/java/com/rawr/config/S3Config.java`

- [ ] **Step 1: Create `OAuthAttributes.java`**

```java
package com.rawr.auth.dto;

import com.rawr.user.OAuthProvider;
import com.rawr.user.User;

import java.util.Map;

public record OAuthAttributes(
        String oauthId,
        String email,
        String name,
        String profileImage,
        OAuthProvider provider
) {
    public static OAuthAttributes ofGoogle(Map<String, Object> attrs) {
        return new OAuthAttributes(
                (String) attrs.get("sub"),
                (String) attrs.get("email"),
                (String) attrs.get("name"),
                (String) attrs.get("picture"),
                OAuthProvider.GOOGLE
        );
    }

    @SuppressWarnings("unchecked")
    public static OAuthAttributes ofKakao(Map<String, Object> attrs) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        return new OAuthAttributes(
                String.valueOf(attrs.get("id")),
                (String) kakaoAccount.get("email"),
                (String) profile.get("nickname"),
                (String) profile.get("profile_image_url"),
                OAuthProvider.KAKAO
        );
    }

    public User toUser() {
        return new User(email, name, profileImage, provider, oauthId);
    }
}
```

- [ ] **Step 2: Create `OAuth2UserService.java`**

```java
package com.rawr.auth;

import com.rawr.auth.dto.OAuthAttributes;
import com.rawr.user.User;
import com.rawr.user.UserRepository;
import com.rawr.user.OAuthProvider;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public OAuth2UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        OAuthAttributes oauthAttrs = switch (registrationId) {
            case "google" -> OAuthAttributes.ofGoogle(attrs);
            case "kakao"  -> OAuthAttributes.ofKakao(attrs);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };

        User user = userRepository
                .findByOauthProviderAndOauthId(oauthAttrs.provider(), oauthAttrs.oauthId())
                .orElseGet(() -> userRepository.save(oauthAttrs.toUser()));

        user.updateProfile(oauthAttrs.name(), oauthAttrs.profileImage());
        userRepository.save(user);

        String jwt = jwtUtil.generate(user);
        return new DefaultOAuth2User(
                Set.of(() -> "ROLE_" + user.getRole().name()),
                Map.of("jwt", jwt, "sub", user.getId().toString()),
                "sub"
        );
    }
}
```

- [ ] **Step 3: Create `AuthController.java`**

```java
package com.rawr.auth;

import com.rawr.user.User;
import com.rawr.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "profileImage", user.getProfileImage() != null ? user.getProfileImage() : "",
                "role", user.getRole()
        ));
    }
}
```

- [ ] **Step 4: Create `JwtAuthFilter.java`**

```java
package com.rawr.config;

import com.rawr.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                Claims claims = jwtUtil.parse(token);
                UUID userId = UUID.fromString(claims.getSubject());
                String role = claims.get("role", String.class);
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 5: Create `SecurityConfig.java`**

```java
package com.rawr.config;

import com.rawr.auth.JwtUtil;
import com.rawr.auth.OAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final OAuth2UserService oAuth2UserService;
    private final String frontendUrl;

    public SecurityConfig(JwtUtil jwtUtil, OAuth2UserService oAuth2UserService,
                          @Value("${rawr.frontend-url}") String frontendUrl) {
        this.jwtUtil = jwtUtil;
        this.oAuth2UserService = oAuth2UserService;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
                .requestMatchers("/api/subscriptions/unsubscribe").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                .successHandler((request, response, authentication) -> {
                    var principal = (org.springframework.security.oauth2.core.user.OAuth2User)
                            authentication.getPrincipal();
                    String jwt = (String) principal.getAttributes().get("jwt");
                    response.sendRedirect(frontendUrl + "/auth/callback?token=" + jwt);
                })
            )
            .addFilterBefore(new JwtAuthFilter(jwtUtil),
                    UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 6: Create `S3Config.java`**

```java
package com.rawr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${rawr.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/auth/ rawr-backend/src/main/java/com/rawr/config/
git commit -m "feat(rawr): add OAuth2 login, JWT auth, security config"
```

---

## Task 6: Article Domain

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/article/Category.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/ArticleStatus.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/Article.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/ArticleRepository.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/dto/ArticleRequest.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/dto/ArticleResponse.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/ArticleService.java`
- Create: `rawr-backend/src/main/java/com/rawr/article/ArticleController.java`
- Create: `rawr-backend/src/test/java/com/rawr/article/ArticleServiceTest.java`
- Create: `rawr-backend/src/test/java/com/rawr/article/ArticleControllerTest.java`

- [ ] **Step 1: Write failing tests for ArticleService**

Create `rawr-backend/src/test/java/com/rawr/article/ArticleServiceTest.java`:

```java
package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ArticleService articleService;

    User author;

    @BeforeEach
    void setUp() {
        author = new User("author@test.com", "Author", null, OAuthProvider.GOOGLE, "google-123");
    }

    @Test
    void createArticle_savesAsDraft() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new ArticleRequest("My Title", "Content here", "cover.jpg", Category.FASHION);
        var response = articleService.create(UUID.randomUUID(), request);

        assertThat(response.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(response.slug()).isEqualTo("my-title");
    }

    @Test
    void publishArticle_setsPublishedStatus() {
        Article article = new Article("Title", "title", "Content", null, Category.FASHION, author);
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = articleService.publish(article.getId());

        assertThat(response.status()).isEqualTo(ArticleStatus.PUBLISHED);
    }

    @Test
    void createArticle_duplicateSlug_appendsNumber() {
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(articleRepository.existsBySlug("my-title")).thenReturn(true);
        when(articleRepository.existsBySlug("my-title-2")).thenReturn(false);
        when(articleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = new ArticleRequest("My Title", "Content", null, Category.CULTURE);
        var response = articleService.create(UUID.randomUUID(), request);

        assertThat(response.slug()).isEqualTo("my-title-2");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.article.ArticleServiceTest" 2>&1 | tail -20
```
Expected: FAIL — classes don't exist yet

- [ ] **Step 3: Create `Category.java`**

```java
package com.rawr.article;

public enum Category { FASHION, CULTURE }
```

- [ ] **Step 4: Create `ArticleStatus.java`**

```java
package com.rawr.article;

public enum ArticleStatus { DRAFT, PUBLISHED }
```

- [ ] **Step 5: Create `Article.java`**

```java
package com.rawr.article;

import com.rawr.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String coverImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "article_category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "article_status")
    private ArticleStatus status = ArticleStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Article() {}

    public Article(String title, String slug, String content,
                   String coverImage, Category category, User author) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.author = author;
    }

    public void update(String title, String slug, String content,
                       String coverImage, Category category) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.coverImage = coverImage;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.status = ArticleStatus.DRAFT;
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public String getContent() { return content; }
    public String getCoverImage() { return coverImage; }
    public Category getCategory() { return category; }
    public ArticleStatus getStatus() { return status; }
    public User getAuthor() { return author; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 6: Create `ArticleRepository.java`**

```java
package com.rawr.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Optional<Article> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);
    Page<Article> findByStatusAndCategory(ArticleStatus status, Category category, Pageable pageable);
    List<Article> findByStatus(ArticleStatus status);
}
```

- [ ] **Step 7: Create `ArticleRequest.java`**

```java
package com.rawr.article.dto;

import com.rawr.article.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleRequest(
        @NotBlank String title,
        @NotBlank String content,
        String coverImage,
        @NotNull Category category
) {}
```

- [ ] **Step 8: Create `ArticleResponse.java`**

```java
package com.rawr.article.dto;

import com.rawr.article.Article;
import com.rawr.article.ArticleStatus;
import com.rawr.article.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String title,
        String slug,
        String content,
        String coverImage,
        Category category,
        ArticleStatus status,
        String authorName,
        UUID authorId,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ArticleResponse from(Article a) {
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getSlug(),
                a.getContent(), a.getCoverImage(), a.getCategory(),
                a.getStatus(), a.getAuthor().getUsername(), a.getAuthor().getId(),
                a.getPublishedAt(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 9: Create `ArticleService.java`**

```java
package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.article.dto.ArticleResponse;
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

    public ArticleService(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
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
        return ArticleResponse.from(articleRepository.save(article));
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
```

- [ ] **Step 10: Create `ArticleController.java`**

```java
package com.rawr.article;

import com.rawr.article.dto.ArticleRequest;
import com.rawr.article.dto.ArticleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public Page<ArticleResponse> list(
            @RequestParam(required = false) Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return articleService.listPublished(category,
                PageRequest.of(page, size, Sort.by("publishedAt").descending()));
    }

    @GetMapping("/{slug}")
    public ArticleResponse get(@PathVariable String slug) {
        return articleService.getBySlug(slug);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ResponseEntity<ArticleResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articleService.create(userId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ArticleResponse update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ArticleRequest request) {
        return articleService.update(id, userId, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('OWNER')")
    public ArticleResponse publish(@PathVariable UUID id) {
        return articleService.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('OWNER')")
    public ArticleResponse unpublish(@PathVariable UUID id) {
        return articleService.unpublish(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        articleService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 11: Run tests to verify they pass**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.article.ArticleServiceTest" 2>&1 | tail -10
```
Expected: 3 tests PASS

- [ ] **Step 12: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/article/ rawr-backend/src/test/java/com/rawr/article/
git commit -m "feat(rawr): add Article domain (CRUD, publish, slug)"
```

---

## Task 7: S3 Image Upload

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/image/ImageService.java`
- Create: `rawr-backend/src/main/java/com/rawr/image/ImageController.java`

- [ ] **Step 1: Create `ImageService.java`**

```java
package com.rawr.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public ImageService(S3Client s3Client,
                        @Value("${rawr.s3.bucket}") String bucket,
                        @Value("${rawr.s3.region}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    public String upload(MultipartFile file) throws IOException {
        String key = "images/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
```

- [ ] **Step 2: Create `ImageController.java`**

```java
package com.rawr.image;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'CONTRIBUTOR')")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file)
            throws IOException {
        String url = imageService.upload(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/image/
git commit -m "feat(rawr): add S3 image upload"
```

---

## Task 8: Comment Domain

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/comment/Comment.java`
- Create: `rawr-backend/src/main/java/com/rawr/comment/CommentRepository.java`
- Create: `rawr-backend/src/main/java/com/rawr/comment/dto/CommentRequest.java`
- Create: `rawr-backend/src/main/java/com/rawr/comment/dto/CommentResponse.java`
- Create: `rawr-backend/src/main/java/com/rawr/comment/CommentService.java`
- Create: `rawr-backend/src/main/java/com/rawr/comment/CommentController.java`
- Create: `rawr-backend/src/test/java/com/rawr/comment/CommentServiceTest.java`

- [ ] **Step 1: Write failing tests for CommentService**

```java
package com.rawr.comment;

import com.rawr.article.*;
import com.rawr.comment.dto.CommentRequest;
import com.rawr.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void addComment_savesComment() {
        var author = new User("a@test.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("T", "t", "C", null, Category.FASHION, author);
        var commenter = new User("b@test.com", "Bob", null, OAuthProvider.GOOGLE, "g2");

        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(commenter));
        when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = commentService.add(UUID.randomUUID(), UUID.randomUUID(),
                new CommentRequest("Great article!"));

        assertThat(response.content()).isEqualTo("Great article!");
        assertThat(response.authorName()).isEqualTo("Bob");
    }

    @Test
    void deleteComment_byOwner_succeeds() {
        var author = new User("a@test.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("T", "t", "C", null, Category.FASHION, author);
        var comment = new Comment("text", article, author);

        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        doNothing().when(commentRepository).delete(any());

        assertThatCode(() -> commentService.delete(UUID.randomUUID(), author.getId()))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.comment.CommentServiceTest" 2>&1 | tail -10
```
Expected: FAIL

- [ ] **Step 3: Create `Comment.java`**

```java
package com.rawr.comment;

import com.rawr.article.Article;
import com.rawr.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Comment() {}

    public Comment(String content, Article article, User user) {
        this.content = content;
        this.article = article;
        this.user = user;
    }

    public UUID getId() { return id; }
    public String getContent() { return content; }
    public Article getArticle() { return article; }
    public User getUser() { return user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Create `CommentRepository.java`**

```java
package com.rawr.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByArticleIdOrderByCreatedAtDesc(UUID articleId);
}
```

- [ ] **Step 5: Create `CommentRequest.java`**

```java
package com.rawr.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(@NotBlank String content) {}
```

- [ ] **Step 6: Create `CommentResponse.java`**

```java
package com.rawr.comment.dto;

import com.rawr.comment.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        String authorName,
        UUID authorId,
        LocalDateTime createdAt
) {
    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getContent(),
                c.getUser().getUsername(), c.getUser().getId(), c.getCreatedAt());
    }
}
```

- [ ] **Step 7: Create `CommentService.java`**

```java
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
```

- [ ] **Step 8: Create `CommentController.java`**

```java
package com.rawr.comment;

import com.rawr.comment.dto.CommentRequest;
import com.rawr.comment.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID articleId) {
        return commentService.list(articleId);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> add(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.add(articleId, userId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID articleId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UUID userId) {
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 9: Run tests to verify they pass**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.comment.CommentServiceTest" 2>&1 | tail -10
```
Expected: 2 tests PASS

- [ ] **Step 10: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/comment/ rawr-backend/src/test/java/com/rawr/comment/
git commit -m "feat(rawr): add Comment domain"
```

---

## Task 9: Like & Bookmark

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/like/Like.java`
- Create: `rawr-backend/src/main/java/com/rawr/like/LikeRepository.java`
- Create: `rawr-backend/src/main/java/com/rawr/like/LikeService.java`
- Create: `rawr-backend/src/main/java/com/rawr/like/LikeController.java`
- Create: `rawr-backend/src/main/java/com/rawr/bookmark/Bookmark.java`
- Create: `rawr-backend/src/main/java/com/rawr/bookmark/BookmarkRepository.java`
- Create: `rawr-backend/src/main/java/com/rawr/bookmark/BookmarkService.java`
- Create: `rawr-backend/src/main/java/com/rawr/bookmark/BookmarkController.java`
- Create: `rawr-backend/src/test/java/com/rawr/like/LikeServiceTest.java`
- Create: `rawr-backend/src/test/java/com/rawr/bookmark/BookmarkServiceTest.java`

- [ ] **Step 1: Write failing tests for LikeService**

```java
package com.rawr.like;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock LikeRepository likeRepository;
    @Mock ArticleRepository articleRepository;
    @Mock UserRepository userRepository;
    @InjectMocks LikeService likeService;

    @Test
    void toggle_whenNotLiked_createsLike() {
        var author = new User("a@t.com", "A", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("T", "t", "C", null, Category.FASHION, author);
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(likeRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(likeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean liked = likeService.toggle(UUID.randomUUID(), UUID.randomUUID());
        assertThat(liked).isTrue();
    }

    @Test
    void toggle_whenAlreadyLiked_removesLike() {
        var author = new User("a@t.com", "A", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("T", "t", "C", null, Category.FASHION, author);
        var like = new Like(article, author);
        when(likeRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.of(like));
        doNothing().when(likeRepository).delete(any());

        boolean liked = likeService.toggle(UUID.randomUUID(), UUID.randomUUID());
        assertThat(liked).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.like.LikeServiceTest" 2>&1 | tail -10
```
Expected: FAIL

- [ ] **Step 3: Create `Like.java`**

```java
package com.rawr.like;

import com.rawr.article.Article;
import com.rawr.user.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "likes")
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Like() {}

    public Like(Article article, User user) {
        this.article = article;
        this.user = user;
    }

    public UUID getId() { return id; }
    public Article getArticle() { return article; }
    public User getUser() { return user; }
}
```

- [ ] **Step 4: Create `LikeRepository.java`**

```java
package com.rawr.like;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByArticleIdAndUserId(UUID articleId, UUID userId);
    long countByArticleId(UUID articleId);
    boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
}
```

- [ ] **Step 5: Create `LikeService.java`**

```java
package com.rawr.like;

import com.rawr.article.ArticleRepository;
import com.rawr.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public LikeService(LikeRepository likeRepository,
                       ArticleRepository articleRepository,
                       UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public boolean toggle(UUID articleId, UUID userId) {
        return likeRepository.findByArticleIdAndUserId(articleId, userId)
                .map(like -> { likeRepository.delete(like); return false; })
                .orElseGet(() -> {
                    var article = articleRepository.findById(articleId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    var user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    likeRepository.save(new Like(article, user));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> status(UUID articleId, UUID userId) {
        return Map.of(
                "count", likeRepository.countByArticleId(articleId),
                "liked", userId != null && likeRepository.existsByArticleIdAndUserId(articleId, userId)
        );
    }
}
```

- [ ] **Step 6: Create `LikeController.java`**

```java
package com.rawr.like;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles/{articleId}/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public Map<String, Object> status(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        return likeService.status(articleId, userId);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        boolean liked = likeService.toggle(articleId, userId);
        return ResponseEntity.ok(Map.of(
                "liked", liked,
                "count", likeService.status(articleId, userId).get("count")
        ));
    }
}
```

- [ ] **Step 7: Create `Bookmark.java`**

```java
package com.rawr.bookmark;

import com.rawr.article.Article;
import com.rawr.user.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Bookmark() {}

    public Bookmark(Article article, User user) {
        this.article = article;
        this.user = user;
    }

    public UUID getId() { return id; }
    public Article getArticle() { return article; }
    public User getUser() { return user; }
}
```

- [ ] **Step 8: Create `BookmarkRepository.java`**

```java
package com.rawr.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByArticleIdAndUserId(UUID articleId, UUID userId);
    List<Bookmark> findByUserIdOrderByIdDesc(UUID userId);
    boolean existsByArticleIdAndUserId(UUID articleId, UUID userId);
}
```

- [ ] **Step 9: Create `BookmarkService.java`**

```java
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
```

- [ ] **Step 10: Create `BookmarkController.java`**

```java
package com.rawr.bookmark;

import com.rawr.article.dto.ArticleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/api/articles/{articleId}/bookmarks")
    public ResponseEntity<Map<String, Boolean>> toggle(
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID userId) {
        boolean bookmarked = bookmarkService.toggle(articleId, userId);
        return ResponseEntity.ok(Map.of("bookmarked", bookmarked));
    }

    @GetMapping("/api/users/me/bookmarks")
    public List<ArticleResponse> myBookmarks(@AuthenticationPrincipal UUID userId) {
        return bookmarkService.myBookmarks(userId);
    }
}
```

- [ ] **Step 11: Write and run BookmarkServiceTest**

```java
package com.rawr.bookmark;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void toggle_whenNotBookmarked_createsBookmark() {
        var author = new User("a@t.com", "A", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("T", "t", "C", null, Category.FASHION, author);
        when(articleRepository.findById(any())).thenReturn(Optional.of(article));
        when(userRepository.findById(any())).thenReturn(Optional.of(author));
        when(bookmarkRepository.findByArticleIdAndUserId(any(), any())).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean bookmarked = bookmarkService.toggle(UUID.randomUUID(), UUID.randomUUID());
        assertThat(bookmarked).isTrue();
    }
}
```

- [ ] **Step 12: Run tests**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.like.LikeServiceTest" --tests "com.rawr.bookmark.BookmarkServiceTest" 2>&1 | tail -10
```
Expected: 3 tests PASS

- [ ] **Step 13: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/like/ rawr-backend/src/main/java/com/rawr/bookmark/ \
        rawr-backend/src/test/java/com/rawr/like/ rawr-backend/src/test/java/com/rawr/bookmark/
git commit -m "feat(rawr): add Like and Bookmark domains"
```

---

## Task 10: Email Subscription

**Files:**
- Create: `rawr-backend/src/main/java/com/rawr/subscription/Subscription.java`
- Create: `rawr-backend/src/main/java/com/rawr/subscription/SubscriptionRepository.java`
- Create: `rawr-backend/src/main/java/com/rawr/subscription/SubscriptionService.java`
- Create: `rawr-backend/src/main/java/com/rawr/subscription/SubscriptionController.java`
- Create: `rawr-backend/src/test/java/com/rawr/subscription/SubscriptionServiceTest.java`

- [ ] **Step 1: Write failing tests for SubscriptionService**

```java
package com.rawr.subscription;

import com.rawr.article.*;
import com.rawr.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock JavaMailSender mailSender;
    @InjectMocks SubscriptionService subscriptionService;

    @Test
    void subscribe_withNewEmail_savesSubscription() {
        when(subscriptionRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> subscriptionService.subscribe("new@test.com")).doesNotThrowAnyException();
        verify(subscriptionRepository).save(any());
    }

    @Test
    void subscribe_withExistingEmail_doesNotDuplicate() {
        var existing = new Subscription("existing@test.com");
        when(subscriptionRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(existing));

        subscriptionService.subscribe("existing@test.com");
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void notifyNewArticle_sendsEmailToAllSubscribers() {
        var author = new User("a@t.com", "Author", null, OAuthProvider.GOOGLE, "g1");
        var article = new Article("Hot Drop", "hot-drop", "Content", null, Category.FASHION, author);
        var sub1 = new Subscription("reader1@test.com");
        var sub2 = new Subscription("reader2@test.com");
        when(subscriptionRepository.findAll()).thenReturn(List.of(sub1, sub2));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        subscriptionService.notifyNewArticle(article);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.subscription.SubscriptionServiceTest" 2>&1 | tail -10
```
Expected: FAIL

- [ ] **Step 3: Create `Subscription.java`**

```java
package com.rawr.subscription;

import com.rawr.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String unsubscribeToken = UUID.randomUUID().toString().replace("-", "");

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Subscription() {}

    public Subscription(String email) { this.email = email; }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Create `SubscriptionRepository.java`**

```java
package com.rawr.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByEmail(String email);
    Optional<Subscription> findByUnsubscribeToken(String token);
}
```

- [ ] **Step 5: Create `SubscriptionService.java`**

```java
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
    private final String frontendUrl;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               JavaMailSender mailSender,
                               @Value("${rawr.frontend-url}") String frontendUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
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
```

- [ ] **Step 6: Update `ArticleServiceTest.java` to add SubscriptionService mock**

`ArticleService` now requires `SubscriptionService`. Add to the existing test class:

```java
@Mock SubscriptionService subscriptionService;
```

The `@InjectMocks ArticleService articleService;` will pick it up automatically via Mockito.

- [ ] **Step 7: Wire notification into ArticleService**

In `ArticleService.java`, inject `SubscriptionService` and call `notifyNewArticle` on publish:

```java
// Add to constructor parameters:
private final SubscriptionService subscriptionService;

// Update constructor:
public ArticleService(ArticleRepository articleRepository,
                      UserRepository userRepository,
                      SubscriptionService subscriptionService) {
    this.articleRepository = articleRepository;
    this.userRepository = userRepository;
    this.subscriptionService = subscriptionService;
}

// Update publish() method:
public ArticleResponse publish(UUID articleId) {
    Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    article.publish();
    Article saved = articleRepository.save(article);
    subscriptionService.notifyNewArticle(saved);
    return ArticleResponse.from(saved);
}
```

- [ ] **Step 8: Create `SubscriptionController.java`**

```java
package com.rawr.subscription;

import jakarta.validation.constraints.Email;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || !email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email"));
        }
        subscriptionService.subscribe(email);
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully"));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String token) {
        subscriptionService.unsubscribe(token);
        return ResponseEntity.ok(Map.of("message", "Unsubscribed successfully"));
    }
}
```

- [ ] **Step 9: Run tests**

```bash
cd rawr-backend && ./gradlew test --tests "com.rawr.subscription.SubscriptionServiceTest" --tests "com.rawr.article.ArticleServiceTest" 2>&1 | tail -10
```
Expected: All tests PASS

- [ ] **Step 10: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/subscription/ \
        rawr-backend/src/main/java/com/rawr/article/ArticleService.java \
        rawr-backend/src/test/java/com/rawr/subscription/ \
        rawr-backend/src/test/java/com/rawr/article/
git commit -m "feat(rawr): add email subscription and new article notifications"
```

---

## Task 11: Contributor Invite

**Files:**
- Modify: `rawr-backend/src/main/java/com/rawr/auth/AuthController.java`
- Modify: `rawr-backend/src/main/java/com/rawr/user/UserRepository.java`

- [ ] **Step 1: Add invite endpoint to `AuthController.java`**

Add this method to the existing `AuthController` class:

```java
@PostMapping("/invite")
@PreAuthorize("hasRole('OWNER')")
public ResponseEntity<Map<String, String>> invite(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    userRepository.findByEmail(email).ifPresent(user -> user.promoteToContributor());
    return ResponseEntity.ok(Map.of("message", "Contributor invited: " + email));
}
```

Also add `@PreAuthorize` import and `import org.springframework.security.access.prepost.PreAuthorize;` at the top of `AuthController.java`.

- [ ] **Step 2: Run full test suite**

```bash
cd rawr-backend && ./gradlew test 2>&1 | tail -20
```
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add rawr-backend/src/main/java/com/rawr/auth/AuthController.java
git commit -m "feat(rawr): add contributor invite endpoint"
```

---

## Task 12: Create PostgreSQL database

- [ ] **Step 1: Create rawr database and user**

```bash
psql -U postgres -c "CREATE USER rawr WITH PASSWORD 'rawr';"
psql -U postgres -c "CREATE DATABASE rawr OWNER rawr;"
```
Expected: `CREATE ROLE` and `CREATE DATABASE`

- [ ] **Step 2: Run application to verify Flyway migration runs cleanly**

```bash
cd rawr-backend && JWT_SECRET=local-dev-secret-minimum-256-bits-here \
  GOOGLE_CLIENT_ID=dummy GOOGLE_CLIENT_SECRET=dummy \
  KAKAO_CLIENT_ID=dummy KAKAO_CLIENT_SECRET=dummy \
  MAIL_USERNAME=dummy MAIL_PASSWORD=dummy \
  S3_BUCKET=dummy S3_REGION=ap-northeast-2 \
  FRONTEND_URL=http://localhost:3000 \
  ./gradlew bootRun 2>&1 | grep -E "(Flyway|Started|ERROR)" | head -10
```
Expected: `Successfully applied 1 migration` and `Started RawrApplication`

- [ ] **Step 3: Commit**

```bash
git add rawr-backend/
git commit -m "feat(rawr): backend implementation complete"
```

---

## Notes

- **Apple Sign In**: Apple OAuth2 uses a JWT-based flow different from standard OAuth2. Implementation is deferred — add `APPLE` as a provider in enums but skip the `OAuthAttributes.ofApple()` case for now. Apple requires a paid Apple Developer account and a registered app.
- **Frontend**: A separate plan `2026-04-13-rawr-frontend.md` will be written after Figma design is complete.
- **Environment variables required at runtime**: `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `S3_BUCKET`, `S3_REGION`, `FRONTEND_URL`
