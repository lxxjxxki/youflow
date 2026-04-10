# youflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** YouTube 영상을 플레이리스트로 관리하고, 영상별 볼륨을 저장하며, 서브모니터에 재생하고 다른 기기에서 실시간 조작할 수 있는 웹 서비스를 구축한다.

**Architecture:** Spring Boot REST API + WebSocket(STOMP) 서버가 룸 상태를 관리하고, React 클라이언트 두 종류(Player View, Controller View)가 WebSocket으로 실시간 동기화된다. 인증은 JWT, 데이터는 PostgreSQL에 저장한다.

**Tech Stack:** Java 21, Spring Boot 3.x, Gradle, PostgreSQL, Flyway, STOMP over WebSocket, jjwt, React 18, Vite, Axios, Zustand, @stomp/stompjs, YouTube iframe API, YouTube Data API v3

---

## 프로젝트 구조

```
youflow/
├── backend/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/youflow/
│       │   ├── YouflowApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── WebSocketConfig.java
│       │   │   └── JwtAuthFilter.java
│       │   ├── auth/
│       │   │   ├── AuthController.java
│       │   │   ├── AuthService.java
│       │   │   ├── JwtUtil.java
│       │   │   └── dto/ (LoginRequest, RegisterRequest, AuthResponse)
│       │   ├── user/
│       │   │   ├── User.java
│       │   │   └── UserRepository.java
│       │   ├── playlist/
│       │   │   ├── Playlist.java
│       │   │   ├── PlaylistRepository.java
│       │   │   ├── PlaylistController.java
│       │   │   └── PlaylistService.java
│       │   ├── video/
│       │   │   ├── Video.java
│       │   │   ├── VideoRepository.java
│       │   │   ├── VideoController.java
│       │   │   ├── VideoService.java
│       │   │   └── YoutubeApiClient.java
│       │   ├── room/
│       │   │   ├── RoomManager.java
│       │   │   └── RoomState.java
│       │   └── websocket/
│       │       ├── WebSocketController.java
│       │       └── dto/ (WsEvent, StateUpdate)
│       └── resources/
│           ├── application.yml
│           └── db/migration/V1__init.sql
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── api/
        │   ├── client.js
        │   ├── auth.js
        │   ├── playlist.js
        │   └── video.js
        ├── hooks/
        │   ├── useWebSocket.js
        │   └── useRoom.js
        ├── store/
        │   └── roomStore.js
        ├── pages/
        │   ├── LoginPage.jsx
        │   ├── RegisterPage.jsx
        │   ├── ControllerPage.jsx
        │   └── PlayerPage.jsx
        └── components/
            ├── controller/
            │   ├── PlaylistPanel.jsx
            │   ├── VideoItem.jsx
            │   ├── PlaybackControls.jsx
            │   └── VolumeSlider.jsx
            └── player/
                ├── YoutubePlayer.jsx
                ├── PlayerControls.jsx
                └── UpNextChip.jsx
```

---

## Part 1 — 프로젝트 기반 설정

### Task 1-1: Spring Boot 프로젝트 초기화

**Files:**
- Create: `backend/build.gradle`
- Create: `backend/src/main/java/com/youflow/YouflowApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: backend 디렉토리 생성 및 Gradle 설정 작성**

```bash
mkdir -p backend/src/main/java/com/youflow
mkdir -p backend/src/main/resources/db/migration
mkdir -p backend/src/test/java/com/youflow
```

`backend/build.gradle`:
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.4'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.youflow'
version = '0.0.1-SNAPSHOT'
java { sourceCompatibility = JavaVersion.VERSION_21 }

repositories { mavenCentral() }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.flywaydb:flyway-core'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
    runtimeOnly 'org.postgresql:postgresql'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

tasks.named('test') { useJUnitPlatform() }
```

- [ ] **Step 2: application.yml 작성**

`backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/youflow
    username: youflow
    password: youflow
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

youflow:
  jwt:
    secret: youflow-secret-key-at-least-256-bits-long-for-hs256
    expiration-ms: 86400000  # 24h
  youtube:
    api-key: ${YOUTUBE_API_KEY}

server:
  port: 8080
```

- [ ] **Step 3: YouflowApplication.java 작성**

`backend/src/main/java/com/youflow/YouflowApplication.java`:
```java
package com.youflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YouflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(YouflowApplication.class, args);
    }
}
```

- [ ] **Step 4: DB 스키마 마이그레이션 파일 작성**

`backend/src/main/resources/db/migration/V1__init.sql`:
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE playlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE videos (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
    youtube_url VARCHAR(500) NOT NULL,
    youtube_id VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    thumbnail VARCHAR(500),
    position INT NOT NULL DEFAULT 0,
    volume INT NOT NULL DEFAULT 70,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_playlist_id ON videos(playlist_id);
CREATE INDEX idx_playlists_user_id ON playlists(user_id);
```

- [ ] **Step 5: PostgreSQL 로컬 DB 생성**

```bash
psql -U postgres -c "CREATE USER youflow WITH PASSWORD 'youflow';"
psql -U postgres -c "CREATE DATABASE youflow OWNER youflow;"
```

- [ ] **Step 6: 빌드 확인**

```bash
cd backend && ./gradlew build -x test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: 커밋**

```bash
git add backend/
git commit -m "feat: initialize Spring Boot backend project"
```

---

### Task 1-2: React 프로젝트 초기화

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/src/main.jsx`
- Create: `frontend/src/App.jsx`

- [ ] **Step 1: Vite + React 프로젝트 생성**

```bash
npm create vite@latest frontend -- --template react
cd frontend && npm install
```

- [ ] **Step 2: 필요한 패키지 설치**

```bash
npm install axios zustand @stomp/stompjs react-router-dom
```

- [ ] **Step 3: vite.config.js — API 프록시 설정**

`frontend/vite.config.js`:
```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'ws://localhost:8080', ws: true }
    }
  }
})
```

- [ ] **Step 4: App.jsx — 라우터 뼈대 작성**

`frontend/src/App.jsx`:
```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ControllerPage from './pages/ControllerPage'
import PlayerPage from './pages/PlayerPage'

function PrivateRoute({ children }) {
  return localStorage.getItem('token') ? children : <Navigate to="/login" />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/controller" element={<PrivateRoute><ControllerPage /></PrivateRoute>} />
        <Route path="/player" element={<PrivateRoute><PlayerPage /></PrivateRoute>} />
        <Route path="/" element={<Navigate to="/controller" />} />
      </Routes>
    </BrowserRouter>
  )
}
```

- [ ] **Step 5: 페이지 플레이스홀더 생성**

`frontend/src/pages/LoginPage.jsx`:
```jsx
export default function LoginPage() { return <div>Login</div> }
```

`frontend/src/pages/RegisterPage.jsx`:
```jsx
export default function RegisterPage() { return <div>Register</div> }
```

`frontend/src/pages/ControllerPage.jsx`:
```jsx
export default function ControllerPage() { return <div>Controller</div> }
```

`frontend/src/pages/PlayerPage.jsx`:
```jsx
export default function PlayerPage() { return <div>Player</div> }
```

- [ ] **Step 6: 개발 서버 실행 확인**

```bash
cd frontend && npm run dev
```
Expected: `http://localhost:5173` 에서 앱 로딩 확인

- [ ] **Step 7: 커밋**

```bash
git add frontend/
git commit -m "feat: initialize React frontend project"
```

---

## Part 2 — 인증 시스템

### Task 2-1: User 엔티티 & JWT 유틸

**Files:**
- Create: `backend/src/main/java/com/youflow/user/User.java`
- Create: `backend/src/main/java/com/youflow/user/UserRepository.java`
- Create: `backend/src/main/java/com/youflow/auth/JwtUtil.java`
- Test: `backend/src/test/java/com/youflow/auth/JwtUtilTest.java`

- [ ] **Step 1: 테스트 작성**

`backend/src/test/java/com/youflow/auth/JwtUtilTest.java`:
```java
package com.youflow.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "test-secret-key-at-least-256-bits-long-for-hs256-algorithm",
            86400000L
        );
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtUtil.generateToken("user@test.com");
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(jwtUtil.validateToken("invalid.token.here")).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.auth.JwtUtilTest"
```
Expected: FAIL — `JwtUtil` not found

- [ ] **Step 3: User 엔티티 작성**

`backend/src/main/java/com/youflow/user/User.java`:
```java
package com.youflow.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected User() {}

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
```

`backend/src/main/java/com/youflow/user/UserRepository.java`:
```java
package com.youflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 4: JwtUtil 구현**

`backend/src/main/java/com/youflow/auth/JwtUtil.java`:
```java
package com.youflow.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
        @Value("${youflow.jwt.secret}") String secret,
        @Value("${youflow.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }
}
```

- [ ] **Step 5: 테스트 실행 — PASS 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.auth.JwtUtilTest"
```
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add backend/src/
git commit -m "feat: add User entity and JwtUtil"
```

---

### Task 2-2: 인증 API (회원가입 / 로그인)

**Files:**
- Create: `backend/src/main/java/com/youflow/auth/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/youflow/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/youflow/auth/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/youflow/auth/AuthService.java`
- Create: `backend/src/main/java/com/youflow/auth/AuthController.java`
- Create: `backend/src/main/java/com/youflow/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/youflow/config/JwtAuthFilter.java`
- Test: `backend/src/test/java/com/youflow/auth/AuthControllerTest.java`

- [ ] **Step 1: 테스트 작성**

`backend/src/test/java/com/youflow/auth/AuthControllerTest.java`:
```java
package com.youflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youflow.auth.dto.LoginRequest;
import com.youflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void registerAndLogin() throws Exception {
        var register = new RegisterRequest("test@youflow.com", "password123");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(register)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        var login = new LoginRequest("test@youflow.com", "password123");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        var register = new RegisterRequest("test2@youflow.com", "password123");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(register)))
            .andExpect(status().isOk());

        var login = new LoginRequest("test2@youflow.com", "wrongpassword");
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(login)))
            .andExpect(status().isUnauthorized());
    }
}
```

`backend/src/main/resources/application-test.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/youflow_test
    username: youflow
    password: youflow
```

```bash
psql -U postgres -c "CREATE DATABASE youflow_test OWNER youflow;"
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.auth.AuthControllerTest"
```
Expected: FAIL — 클래스 없음

- [ ] **Step 3: DTO 작성**

`backend/src/main/java/com/youflow/auth/dto/RegisterRequest.java`:
```java
package com.youflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {}
```

`backend/src/main/java/com/youflow/auth/dto/LoginRequest.java`:
```java
package com.youflow.auth.dto;

public record LoginRequest(String email, String password) {}
```

`backend/src/main/java/com/youflow/auth/dto/AuthResponse.java`:
```java
package com.youflow.auth.dto;

public record AuthResponse(String token) {}
```

- [ ] **Step 4: AuthService 구현**

`backend/src/main/java/com/youflow/auth/AuthService.java`:
```java
package com.youflow.auth;

import com.youflow.auth.dto.*;
import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        var user = new User(req.email(), passwordEncoder.encode(req.password()));
        userRepository.save(user);
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest req) {
        var user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }
}
```

- [ ] **Step 5: AuthController 구현**

`backend/src/main/java/com/youflow/auth/AuthController.java`:
```java
package com.youflow.auth;

import com.youflow.auth.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
```

- [ ] **Step 6: JwtAuthFilter 구현**

`backend/src/main/java/com/youflow/config/JwtAuthFilter.java`:
```java
package com.youflow.config;

import com.youflow.auth.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmail(token);
                var auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 7: SecurityConfig 구현**

`backend/src/main/java/com/youflow/config/SecurityConfig.java`:
```java
package com.youflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

- [ ] **Step 8: 테스트 실행 — PASS 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.auth.AuthControllerTest"
```
Expected: PASS

- [ ] **Step 9: 커밋**

```bash
git add backend/src/
git commit -m "feat: add auth API (register/login) with JWT"
```

---

### Task 2-3: 프론트엔드 인증 화면

**Files:**
- Create: `frontend/src/api/client.js`
- Create: `frontend/src/api/auth.js`
- Modify: `frontend/src/pages/LoginPage.jsx`
- Modify: `frontend/src/pages/RegisterPage.jsx`

- [ ] **Step 1: Axios 클라이언트 설정**

`frontend/src/api/client.js`:
```js
import axios from 'axios'

const client = axios.create({ baseURL: '/api' })

client.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default client
```

`frontend/src/api/auth.js`:
```js
import client from './client'

export const register = (email, password) =>
  client.post('/auth/register', { email, password }).then(r => r.data)

export const login = (email, password) =>
  client.post('/auth/login', { email, password }).then(r => r.data)
```

- [ ] **Step 2: LoginPage 구현**

`frontend/src/pages/LoginPage.jsx`:
```jsx
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../api/auth'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async e => {
    e.preventDefault()
    try {
      const { token } = await login(email, password)
      localStorage.setItem('token', token)
      navigate('/controller')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24 }}>
      <h1>youflow</h1>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input type="email" placeholder="이메일" value={email} onChange={e => setEmail(e.target.value)} required />
        <input type="password" placeholder="비밀번호" value={password} onChange={e => setPassword(e.target.value)} required />
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit">로그인</button>
      </form>
      <p><Link to="/register">회원가입</Link></p>
    </div>
  )
}
```

- [ ] **Step 3: RegisterPage 구현**

`frontend/src/pages/RegisterPage.jsx`:
```jsx
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../api/auth'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async e => {
    e.preventDefault()
    try {
      const { token } = await register(email, password)
      localStorage.setItem('token', token)
      navigate('/controller')
    } catch {
      setError('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.')
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 24 }}>
      <h1>youflow 회원가입</h1>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input type="email" placeholder="이메일" value={email} onChange={e => setEmail(e.target.value)} required />
        <input type="password" placeholder="비밀번호 (8자 이상)" value={password} onChange={e => setPassword(e.target.value)} minLength={8} required />
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit">회원가입</button>
      </form>
      <p><Link to="/login">로그인으로 이동</Link></p>
    </div>
  )
}
```

- [ ] **Step 4: 브라우저에서 회원가입 → 로그인 흐름 수동 확인**

```bash
cd frontend && npm run dev
# http://localhost:5173/register 에서 회원가입 후 /controller 로 이동 확인
```

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/
git commit -m "feat: add login and register pages"
```

---

## Part 3 — 플레이리스트 & 영상 관리

### Task 3-1: Playlist & Video 엔티티 및 CRUD API

**Files:**
- Create: `backend/src/main/java/com/youflow/playlist/Playlist.java`
- Create: `backend/src/main/java/com/youflow/playlist/PlaylistRepository.java`
- Create: `backend/src/main/java/com/youflow/playlist/PlaylistController.java`
- Create: `backend/src/main/java/com/youflow/playlist/PlaylistService.java`
- Create: `backend/src/main/java/com/youflow/video/Video.java`
- Create: `backend/src/main/java/com/youflow/video/VideoRepository.java`
- Test: `backend/src/test/java/com/youflow/playlist/PlaylistControllerTest.java`

- [ ] **Step 1: 테스트 작성**

`backend/src/test/java/com/youflow/playlist/PlaylistControllerTest.java`:
```java
package com.youflow.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youflow.auth.JwtUtil;
import com.youflow.user.User;
import com.youflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlaylistControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;

    String token;

    @BeforeEach
    void setUp() {
        var user = new User("playlist-test@youflow.com", passwordEncoder.encode("password123"));
        userRepository.save(user);
        token = jwtUtil.generateToken(user.getEmail());
    }

    @Test
    void createAndListPlaylist() throws Exception {
        mvc.perform(post("/api/playlists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("name", "카페 BGM"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value("카페 BGM"));

        mvc.perform(get("/api/playlists")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("카페 BGM"));
    }

    @Test
    void deletePlaylist() throws Exception {
        var result = mvc.perform(post("/api/playlists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("name", "삭제할 목록"))))
            .andExpect(status().isOk())
            .andReturn();

        var id = om.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(delete("/api/playlists/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.playlist.PlaylistControllerTest"
```
Expected: FAIL

- [ ] **Step 3: Playlist 엔티티 작성**

`backend/src/main/java/com/youflow/playlist/Playlist.java`:
```java
package com.youflow.playlist;

import com.youflow.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "playlists")
public class Playlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Playlist() {}

    public Playlist(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
}
```

`backend/src/main/java/com/youflow/playlist/PlaylistRepository.java`:
```java
package com.youflow.playlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserEmailOrderByCreatedAtDesc(String email);
}
```

- [ ] **Step 4: Video 엔티티 작성**

`backend/src/main/java/com/youflow/video/Video.java`:
```java
package com.youflow.video;

import com.youflow.playlist.Playlist;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Column(nullable = false)
    private String youtubeUrl;

    @Column(nullable = false)
    private String youtubeId;

    @Column(nullable = false)
    private String title;

    private String thumbnail;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int volume = 70;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Video() {}

    public Video(Playlist playlist, String youtubeUrl, String youtubeId, String title, String thumbnail, int position) {
        this.playlist = playlist;
        this.youtubeUrl = youtubeUrl;
        this.youtubeId = youtubeId;
        this.title = title;
        this.thumbnail = thumbnail;
        this.position = position;
    }

    public Long getId() { return id; }
    public Playlist getPlaylist() { return playlist; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public String getYoutubeId() { return youtubeId; }
    public String getTitle() { return title; }
    public String getThumbnail() { return thumbnail; }
    public int getPosition() { return position; }
    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }
    public void setPosition(int position) { this.position = position; }
}
```

`backend/src/main/java/com/youflow/video/VideoRepository.java`:
```java
package com.youflow.video;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByPlaylistIdOrderByPosition(Long playlistId);
    int countByPlaylistId(Long playlistId);
}
```

- [ ] **Step 5: PlaylistService & PlaylistController 구현**

`backend/src/main/java/com/youflow/playlist/PlaylistService.java`:
```java
package com.youflow.playlist;

import com.youflow.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    public PlaylistService(PlaylistRepository playlistRepository, UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.userRepository = userRepository;
    }

    public Playlist create(String email, String name) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return playlistRepository.save(new Playlist(user, name));
    }

    public List<Playlist> listByUser(String email) {
        return playlistRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    public void delete(String email, Long playlistId) {
        var playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!playlist.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        playlistRepository.delete(playlist);
    }
}
```

`backend/src/main/java/com/youflow/playlist/PlaylistController.java`:
```java
package com.youflow.playlist;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) { this.playlistService = playlistService; }

    @PostMapping
    public PlaylistResponse create(@AuthenticationPrincipal String email,
                                   @RequestBody Map<String, String> body) {
        return PlaylistResponse.from(playlistService.create(email, body.get("name")));
    }

    @GetMapping
    public List<PlaylistResponse> list(@AuthenticationPrincipal String email) {
        return playlistService.listByUser(email).stream().map(PlaylistResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String email, @PathVariable Long id) {
        playlistService.delete(email, id);
        return ResponseEntity.noContent().build();
    }
}
```

`backend/src/main/java/com/youflow/playlist/PlaylistResponse.java`:
```java
package com.youflow.playlist;

public record PlaylistResponse(Long id, String name) {
    public static PlaylistResponse from(Playlist p) {
        return new PlaylistResponse(p.getId(), p.getName());
    }
}
```

- [ ] **Step 6: 테스트 실행 — PASS 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.playlist.PlaylistControllerTest"
```
Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add backend/src/
git commit -m "feat: add Playlist and Video entities with CRUD API"
```

---

### Task 3-2: YouTube 메타데이터 수집 & 영상 추가/볼륨/순서 API

**Files:**
- Create: `backend/src/main/java/com/youflow/video/YoutubeApiClient.java`
- Create: `backend/src/main/java/com/youflow/video/VideoService.java`
- Create: `backend/src/main/java/com/youflow/video/VideoController.java`
- Create: `backend/src/main/java/com/youflow/video/VideoResponse.java`
- Test: `backend/src/test/java/com/youflow/video/VideoControllerTest.java`

- [ ] **Step 1: YoutubeApiClient 구현**

`backend/src/main/java/com/youflow/video/YoutubeApiClient.java`:
```java
package com.youflow.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class YoutubeApiClient {
    private static final Pattern VIDEO_ID_PATTERN =
        Pattern.compile("(?:v=|youtu\\.be/|embed/)([\\w-]{11})");
    private static final Pattern PLAYLIST_ID_PATTERN =
        Pattern.compile("[?&]list=([\\w-]+)");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    public YoutubeApiClient(@Value("${youflow.youtube.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public record VideoMeta(String videoId, String title, String thumbnail) {}

    public VideoMeta fetchVideoMeta(String url) {
        String videoId = extractVideoId(url);
        String apiUrl = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
            .queryParam("id", videoId)
            .queryParam("part", "snippet")
            .queryParam("key", apiKey)
            .toUriString();

        var response = restTemplate.getForObject(apiUrl, Map.class);
        var items = (List<Map<String, Object>>) response.get("items");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Video not found: " + url);

        var snippet = (Map<String, Object>) items.get(0).get("snippet");
        var thumbnails = (Map<String, Object>) snippet.get("thumbnails");
        var medium = (Map<String, Object>) thumbnails.get("medium");

        return new VideoMeta(videoId, (String) snippet.get("title"), (String) medium.get("url"));
    }

    public List<VideoMeta> fetchPlaylistVideos(String playlistUrl) {
        String playlistId = extractPlaylistId(playlistUrl);
        String nextPageToken = null;
        List<VideoMeta> results = new java.util.ArrayList<>();

        do {
            var builder = UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/youtube/v3/playlistItems")
                .queryParam("playlistId", playlistId)
                .queryParam("part", "snippet")
                .queryParam("maxResults", 50)
                .queryParam("key", apiKey);
            if (nextPageToken != null) builder.queryParam("pageToken", nextPageToken);

            var response = restTemplate.getForObject(builder.toUriString(), Map.class);
            var items = (List<Map<String, Object>>) response.get("items");
            nextPageToken = (String) response.get("nextPageToken");

            for (var item : items) {
                var snippet = (Map<String, Object>) item.get("snippet");
                var resourceId = (Map<String, Object>) snippet.get("resourceId");
                String videoId = (String) resourceId.get("videoId");
                var thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                var medium = (Map<String, Object>) thumbnails.getOrDefault("medium", thumbnails.get("default"));
                results.add(new VideoMeta(videoId,
                    (String) snippet.get("title"),
                    medium != null ? (String) medium.get("url") : null));
            }
        } while (nextPageToken != null);

        return results;
    }

    private String extractVideoId(String url) {
        var matcher = VIDEO_ID_PATTERN.matcher(url);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid YouTube URL: " + url);
        return matcher.group(1);
    }

    private String extractPlaylistId(String url) {
        var matcher = PLAYLIST_ID_PATTERN.matcher(url);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid playlist URL: " + url);
        return matcher.group(1);
    }
}
```

- [ ] **Step 2: VideoService 구현**

`backend/src/main/java/com/youflow/video/VideoService.java`:
```java
package com.youflow.video;

import com.youflow.playlist.PlaylistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class VideoService {
    private final VideoRepository videoRepository;
    private final PlaylistRepository playlistRepository;
    private final YoutubeApiClient youtubeApiClient;

    public VideoService(VideoRepository videoRepository, PlaylistRepository playlistRepository,
                        YoutubeApiClient youtubeApiClient) {
        this.videoRepository = videoRepository;
        this.playlistRepository = playlistRepository;
        this.youtubeApiClient = youtubeApiClient;
    }

    public Video addVideo(String email, Long playlistId, String url) {
        var playlist = getPlaylistOwnedBy(email, playlistId);
        var meta = youtubeApiClient.fetchVideoMeta(url);
        int position = videoRepository.countByPlaylistId(playlistId);
        var video = new Video(playlist, url, meta.videoId(), meta.title(), meta.thumbnail(), position);
        return videoRepository.save(video);
    }

    public List<Video> importPlaylist(String email, Long playlistId, String playlistUrl) {
        var playlist = getPlaylistOwnedBy(email, playlistId);
        var metas = youtubeApiClient.fetchPlaylistVideos(playlistUrl);
        int startPosition = videoRepository.countByPlaylistId(playlistId);
        return metas.stream().map(meta -> {
            String url = "https://www.youtube.com/watch?v=" + meta.videoId();
            var video = new Video(playlist, url, meta.videoId(), meta.title(), meta.thumbnail(), startPosition + metas.indexOf(meta));
            return videoRepository.save(video);
        }).toList();
    }

    public List<Video> listVideos(String email, Long playlistId) {
        getPlaylistOwnedBy(email, playlistId);
        return videoRepository.findByPlaylistIdOrderByPosition(playlistId);
    }

    public Video updateVolume(String email, Long videoId, int volume) {
        var video = getVideoOwnedBy(email, videoId);
        video.setVolume(volume);
        return video;
    }

    public void deleteVideo(String email, Long videoId) {
        var video = getVideoOwnedBy(email, videoId);
        videoRepository.delete(video);
    }

    public void reorder(String email, Long playlistId, List<Long> orderedVideoIds) {
        getPlaylistOwnedBy(email, playlistId);
        for (int i = 0; i < orderedVideoIds.size(); i++) {
            var video = videoRepository.findById(orderedVideoIds.get(i))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            video.setPosition(i);
        }
    }

    private com.youflow.playlist.Playlist getPlaylistOwnedBy(String email, Long playlistId) {
        var playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!playlist.getUser().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return playlist;
    }

    private Video getVideoOwnedBy(String email, Long videoId) {
        var video = videoRepository.findById(videoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!video.getPlaylist().getUser().getEmail().equals(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return video;
    }
}
```

- [ ] **Step 3: VideoController 구현**

`backend/src/main/java/com/youflow/video/VideoController.java`:
```java
package com.youflow.video;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VideoController {
    private final VideoService videoService;

    public VideoController(VideoService videoService) { this.videoService = videoService; }

    @GetMapping("/playlists/{playlistId}/videos")
    public List<VideoResponse> list(@AuthenticationPrincipal String email, @PathVariable Long playlistId) {
        return videoService.listVideos(email, playlistId).stream().map(VideoResponse::from).toList();
    }

    @PostMapping("/playlists/{playlistId}/videos")
    public VideoResponse addVideo(@AuthenticationPrincipal String email,
                                  @PathVariable Long playlistId,
                                  @RequestBody Map<String, String> body) {
        return VideoResponse.from(videoService.addVideo(email, playlistId, body.get("url")));
    }

    @PostMapping("/playlists/{playlistId}/import")
    public List<VideoResponse> importPlaylist(@AuthenticationPrincipal String email,
                                              @PathVariable Long playlistId,
                                              @RequestBody Map<String, String> body) {
        return videoService.importPlaylist(email, playlistId, body.get("playlistUrl"))
            .stream().map(VideoResponse::from).toList();
    }

    @PatchMapping("/videos/{videoId}/volume")
    public VideoResponse updateVolume(@AuthenticationPrincipal String email,
                                      @PathVariable Long videoId,
                                      @RequestBody Map<String, Integer> body) {
        return VideoResponse.from(videoService.updateVolume(email, videoId, body.get("volume")));
    }

    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String email, @PathVariable Long videoId) {
        videoService.deleteVideo(email, videoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/playlists/{playlistId}/reorder")
    public ResponseEntity<Void> reorder(@AuthenticationPrincipal String email,
                                         @PathVariable Long playlistId,
                                         @RequestBody Map<String, List<Long>> body) {
        videoService.reorder(email, playlistId, body.get("videoIds"));
        return ResponseEntity.ok().build();
    }
}
```

`backend/src/main/java/com/youflow/video/VideoResponse.java`:
```java
package com.youflow.video;

public record VideoResponse(Long id, String youtubeId, String youtubeUrl, String title, String thumbnail, int position, int volume) {
    public static VideoResponse from(Video v) {
        return new VideoResponse(v.getId(), v.getYoutubeId(), v.getYoutubeUrl(), v.getTitle(), v.getThumbnail(), v.getPosition(), v.getVolume());
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
cd backend && ./gradlew build -x test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add backend/src/
git commit -m "feat: add video CRUD, YouTube API integration, playlist import"
```

---

## Part 4 — WebSocket 실시간 통신

### Task 4-1: WebSocket 서버 & 룸 상태 관리

**Files:**
- Create: `backend/src/main/java/com/youflow/config/WebSocketConfig.java`
- Create: `backend/src/main/java/com/youflow/room/RoomState.java`
- Create: `backend/src/main/java/com/youflow/room/RoomManager.java`
- Create: `backend/src/main/java/com/youflow/websocket/dto/WsEvent.java`
- Create: `backend/src/main/java/com/youflow/websocket/dto/StateUpdate.java`
- Create: `backend/src/main/java/com/youflow/websocket/WebSocketController.java`
- Test: `backend/src/test/java/com/youflow/room/RoomManagerTest.java`

- [ ] **Step 1: 테스트 작성**

`backend/src/test/java/com/youflow/room/RoomManagerTest.java`:
```java
package com.youflow.room;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RoomManagerTest {
    private RoomManager roomManager;

    @BeforeEach
    void setUp() { roomManager = new RoomManager(); }

    @Test
    void getOrCreateRoomReturnsConsistentState() {
        var state1 = roomManager.getOrCreate("user@test.com");
        var state2 = roomManager.getOrCreate("user@test.com");
        assertThat(state1).isSameAs(state2);
    }

    @Test
    void updateCurrentVideoIndexUpdatesState() {
        var state = roomManager.getOrCreate("user@test.com");
        state.setCurrentIndex(3);
        assertThat(roomManager.getOrCreate("user@test.com").getCurrentIndex()).isEqualTo(3);
    }
}
```

- [ ] **Step 2: 테스트 실행 — FAIL 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.room.RoomManagerTest"
```
Expected: FAIL

- [ ] **Step 3: RoomState & RoomManager 구현**

`backend/src/main/java/com/youflow/room/RoomState.java`:
```java
package com.youflow.room;

import com.youflow.video.VideoResponse;
import java.util.List;

public class RoomState {
    private int currentIndex = 0;
    private boolean playing = false;
    private List<VideoResponse> playlist = List.of();
    private Long currentPlaylistId;

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
    public boolean isPlaying() { return playing; }
    public void setPlaying(boolean playing) { this.playing = playing; }
    public List<VideoResponse> getPlaylist() { return playlist; }
    public void setPlaylist(List<VideoResponse> playlist) { this.playlist = playlist; }
    public Long getCurrentPlaylistId() { return currentPlaylistId; }
    public void setCurrentPlaylistId(Long currentPlaylistId) { this.currentPlaylistId = currentPlaylistId; }

    public VideoResponse currentVideo() {
        if (playlist.isEmpty() || currentIndex >= playlist.size()) return null;
        return playlist.get(currentIndex);
    }
}
```

`backend/src/main/java/com/youflow/room/RoomManager.java`:
```java
package com.youflow.room;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {
    private final ConcurrentHashMap<String, RoomState> rooms = new ConcurrentHashMap<>();

    public RoomState getOrCreate(String userEmail) {
        return rooms.computeIfAbsent(userEmail, k -> new RoomState());
    }
}
```

- [ ] **Step 4: 테스트 실행 — PASS 확인**

```bash
cd backend && ./gradlew test --tests "com.youflow.room.RoomManagerTest"
```
Expected: PASS

- [ ] **Step 5: WebSocket 설정**

`backend/src/main/java/com/youflow/config/WebSocketConfig.java`:
```java
package com.youflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
    }
}
```

- [ ] **Step 6: WsEvent & StateUpdate DTO**

`backend/src/main/java/com/youflow/websocket/dto/WsEvent.java`:
```java
package com.youflow.websocket.dto;

public record WsEvent(String type, Object payload) {}
```

`backend/src/main/java/com/youflow/websocket/dto/StateUpdate.java`:
```java
package com.youflow.websocket.dto;

import com.youflow.video.VideoResponse;
import java.util.List;

public record StateUpdate(
    VideoResponse currentVideo,
    int currentIndex,
    boolean playing,
    List<VideoResponse> playlist
) {}
```

- [ ] **Step 7: WebSocketController 구현**

`backend/src/main/java/com/youflow/websocket/WebSocketController.java`:
```java
package com.youflow.websocket;

import com.youflow.room.RoomManager;
import com.youflow.video.VideoResponse;
import com.youflow.video.VideoService;
import com.youflow.websocket.dto.StateUpdate;
import com.youflow.websocket.dto.WsEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {
    private final RoomManager roomManager;
    private final SimpMessagingTemplate messaging;
    private final VideoService videoService;

    public WebSocketController(RoomManager roomManager, SimpMessagingTemplate messaging, VideoService videoService) {
        this.roomManager = roomManager;
        this.messaging = messaging;
        this.videoService = videoService;
    }

    @MessageMapping("/play")
    public void play(Principal principal, Map<String, Object> payload) {
        var state = roomManager.getOrCreate(principal.getName());
        state.setPlaying(true);
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/pause")
    public void pause(Principal principal) {
        var state = roomManager.getOrCreate(principal.getName());
        state.setPlaying(false);
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/next")
    public void next(Principal principal) {
        var state = roomManager.getOrCreate(principal.getName());
        if (state.getCurrentIndex() < state.getPlaylist().size() - 1) {
            state.setCurrentIndex(state.getCurrentIndex() + 1);
            state.setPlaying(true);
        }
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/prev")
    public void prev(Principal principal) {
        var state = roomManager.getOrCreate(principal.getName());
        if (state.getCurrentIndex() > 0) {
            state.setCurrentIndex(state.getCurrentIndex() - 1);
            state.setPlaying(true);
        }
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/select")
    public void select(Principal principal, Map<String, Integer> payload) {
        var state = roomManager.getOrCreate(principal.getName());
        state.setCurrentIndex(payload.get("index"));
        state.setPlaying(true);
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/load-playlist")
    public void loadPlaylist(Principal principal, Map<String, Long> payload) {
        var state = roomManager.getOrCreate(principal.getName());
        Long playlistId = payload.get("playlistId");
        List<VideoResponse> videos = videoService.listVideos(principal.getName(), playlistId)
            .stream().map(VideoResponse::from).toList();
        state.setPlaylist(videos);
        state.setCurrentPlaylistId(playlistId);
        state.setCurrentIndex(0);
        state.setPlaying(false);
        broadcast(principal.getName(), state);
    }

    @MessageMapping("/volume-changed")
    public void volumeChanged(Principal principal, Map<String, Object> payload) {
        Long videoId = Long.valueOf(payload.get("videoId").toString());
        int volume = (int) payload.get("volume");
        videoService.updateVolume(principal.getName(), videoId, volume);
        var state = roomManager.getOrCreate(principal.getName());
        // 플레이리스트 내 볼륨값 갱신
        state.setPlaylist(state.getPlaylist().stream().map(v ->
            v.id().equals(videoId) ? new VideoResponse(v.id(), v.youtubeId(), v.youtubeUrl(), v.title(), v.thumbnail(), v.position(), volume) : v
        ).toList());
        broadcast(principal.getName(), state);
    }

    private void broadcast(String email, com.youflow.room.RoomState state) {
        String topic = "/topic/room/" + email.replace("@", "-").replace(".", "-");
        messaging.convertAndSend(topic, new StateUpdate(
            state.currentVideo(),
            state.getCurrentIndex(),
            state.isPlaying(),
            state.getPlaylist()
        ));
    }
}
```

- [ ] **Step 8: 빌드 확인**

```bash
cd backend && ./gradlew build -x test
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: 커밋**

```bash
git add backend/src/
git commit -m "feat: add WebSocket room management and real-time state broadcasting"
```

---

## Part 5 — Controller View (React)

### Task 5-1: API 클라이언트 & 상태 관리

**Files:**
- Create: `frontend/src/api/playlist.js`
- Create: `frontend/src/api/video.js`
- Create: `frontend/src/store/roomStore.js`
- Create: `frontend/src/hooks/useWebSocket.js`

- [ ] **Step 1: API 클라이언트 작성**

`frontend/src/api/playlist.js`:
```js
import client from './client'

export const getPlaylists = () => client.get('/playlists').then(r => r.data)
export const createPlaylist = name => client.post('/playlists', { name }).then(r => r.data)
export const deletePlaylist = id => client.delete(`/playlists/${id}`)
export const getVideos = playlistId => client.get(`/playlists/${playlistId}/videos`).then(r => r.data)
```

`frontend/src/api/video.js`:
```js
import client from './client'

export const addVideo = (playlistId, url) =>
  client.post(`/playlists/${playlistId}/videos`, { url }).then(r => r.data)

export const importPlaylist = (playlistId, playlistUrl) =>
  client.post(`/playlists/${playlistId}/import`, { playlistUrl }).then(r => r.data)

export const updateVolume = (videoId, volume) =>
  client.patch(`/videos/${videoId}/volume`, { volume }).then(r => r.data)

export const deleteVideo = videoId => client.delete(`/videos/${videoId}`)

export const reorderVideos = (playlistId, videoIds) =>
  client.patch(`/playlists/${playlistId}/reorder`, { videoIds })
```

- [ ] **Step 2: Zustand 룸 스토어 작성**

`frontend/src/store/roomStore.js`:
```js
import { create } from 'zustand'

export const useRoomStore = create(set => ({
  currentVideo: null,
  currentIndex: 0,
  playing: false,
  playlist: [],
  setStateUpdate: update => set({
    currentVideo: update.currentVideo,
    currentIndex: update.currentIndex,
    playing: update.playing,
    playlist: update.playlist,
  }),
}))
```

- [ ] **Step 3: WebSocket 훅 작성**

`frontend/src/hooks/useWebSocket.js`:
```js
import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useRoomStore } from '../store/roomStore'

export function useWebSocket() {
  const clientRef = useRef(null)
  const setStateUpdate = useRoomStore(s => s.setStateUpdate)

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) return

    const email = JSON.parse(atob(token.split('.')[1])).sub
    const topic = `/topic/room/${email.replace('@', '-').replace(/\./g, '-')}`

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      onConnect: () => {
        client.subscribe(topic, msg => {
          setStateUpdate(JSON.parse(msg.body))
        })
      },
      reconnectDelay: 3000,
    })

    client.activate()
    clientRef.current = client

    return () => client.deactivate()
  }, [])

  const send = (destination, body = {}) => {
    clientRef.current?.publish({
      destination: `/app${destination}`,
      body: JSON.stringify(body),
    })
  }

  return { send }
}
```

- [ ] **Step 4: 커밋**

```bash
git add frontend/src/
git commit -m "feat: add API clients, room store, and WebSocket hook"
```

---

### Task 5-2: Controller View UI

**Files:**
- Create: `frontend/src/components/controller/PlaylistPanel.jsx`
- Create: `frontend/src/components/controller/VideoItem.jsx`
- Create: `frontend/src/components/controller/PlaybackControls.jsx`
- Modify: `frontend/src/pages/ControllerPage.jsx`

- [ ] **Step 1: PlaybackControls 컴포넌트**

`frontend/src/components/controller/PlaybackControls.jsx`:
```jsx
import { useRoomStore } from '../../store/roomStore'

export default function PlaybackControls({ send }) {
  const { currentVideo, playing } = useRoomStore()

  return (
    <div style={{ padding: '16px', borderTop: '1px solid #333', background: '#111' }}>
      <div style={{ color: '#aaa', fontSize: 13, marginBottom: 8 }}>
        {currentVideo ? `재생 중: ${currentVideo.title}` : '재생 중인 영상 없음'}
      </div>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
        <button onClick={() => send('/prev')}>⏮</button>
        <button onClick={() => send(playing ? '/pause' : '/play')}>
          {playing ? '⏸' : '▶'}
        </button>
        <button onClick={() => send('/next')}>⏭</button>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: VideoItem 컴포넌트**

`frontend/src/components/controller/VideoItem.jsx`:
```jsx
import { useState } from 'react'
import { updateVolume } from '../../api/video'

export default function VideoItem({ video, index, isActive, onSelect, send }) {
  const [vol, setVol] = useState(video.volume)

  const handleVolumeChange = async e => {
    const newVol = Number(e.target.value)
    setVol(newVol)
    await updateVolume(video.id, newVol)
    send('/volume-changed', { videoId: video.id, volume: newVol })
  }

  return (
    <div onClick={() => onSelect(index)}
      style={{
        display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px',
        background: isActive ? '#1a1a1a' : 'transparent',
        cursor: 'pointer', borderBottom: '1px solid #222'
      }}>
      {video.thumbnail && <img src={video.thumbnail} alt="" style={{ width: 64, height: 36, objectFit: 'cover', borderRadius: 4 }} />}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13, color: '#fff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {video.title}
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontSize: 11, color: '#888' }}>🔊</span>
        <input type="range" min={0} max={100} value={vol}
          onChange={handleVolumeChange}
          onClick={e => e.stopPropagation()}
          style={{ width: 72 }} />
        <span style={{ fontSize: 11, color: '#888', width: 24 }}>{vol}</span>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: PlaylistPanel 컴포넌트**

`frontend/src/components/controller/PlaylistPanel.jsx`:
```jsx
import { useState, useEffect } from 'react'
import { getVideos, addVideo, importPlaylist, deleteVideo } from '../../api/video'
import { useRoomStore } from '../../store/roomStore'
import VideoItem from './VideoItem'

export default function PlaylistPanel({ playlistId, send }) {
  const [videos, setVideos] = useState([])
  const [urlInput, setUrlInput] = useState('')
  const [importing, setImporting] = useState(false)
  const { currentIndex } = useRoomStore()

  useEffect(() => {
    if (!playlistId) return
    getVideos(playlistId).then(setVideos)
    send('/load-playlist', { playlistId })
  }, [playlistId])

  const handleAddVideo = async e => {
    e.preventDefault()
    if (!urlInput.trim()) return
    const video = await addVideo(playlistId, urlInput.trim())
    setVideos(prev => [...prev, video])
    setUrlInput('')
  }

  const handleImport = async e => {
    e.preventDefault()
    if (!urlInput.trim()) return
    setImporting(true)
    const newVideos = await importPlaylist(playlistId, urlInput.trim())
    setVideos(prev => [...prev, ...newVideos])
    setUrlInput('')
    setImporting(false)
  }

  const handleSelect = index => {
    send('/select', { index })
  }

  return (
    <div style={{ flex: 1, overflowY: 'auto' }}>
      <div style={{ overflowY: 'auto', flex: 1 }}>
        {videos.map((v, i) => (
          <VideoItem key={v.id} video={v} index={i} isActive={i === currentIndex}
            onSelect={handleSelect} send={send} />
        ))}
      </div>
      <form style={{ display: 'flex', gap: 8, padding: 12, borderTop: '1px solid #333' }}>
        <input value={urlInput} onChange={e => setUrlInput(e.target.value)}
          placeholder="YouTube URL 또는 플레이리스트 URL"
          style={{ flex: 1, background: '#222', border: '1px solid #444', color: '#fff', padding: '6px 10px', borderRadius: 4 }} />
        <button type="button" onClick={handleAddVideo}>+ 영상</button>
        <button type="button" onClick={handleImport} disabled={importing}>
          {importing ? '가져오는 중...' : '+ 플레이리스트'}
        </button>
      </form>
    </div>
  )
}
```

- [ ] **Step 4: ControllerPage 조립**

`frontend/src/pages/ControllerPage.jsx`:
```jsx
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getPlaylists, createPlaylist } from '../api/playlist'
import PlaylistPanel from '../components/controller/PlaylistPanel'
import PlaybackControls from '../components/controller/PlaybackControls'
import { useWebSocket } from '../hooks/useWebSocket'

export default function ControllerPage() {
  const [playlists, setPlaylists] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [newName, setNewName] = useState('')
  const navigate = useNavigate()
  const { send } = useWebSocket()

  useEffect(() => {
    getPlaylists().then(data => {
      setPlaylists(data)
      if (data.length > 0) setSelectedId(data[0].id)
    })
  }, [])

  const handleCreate = async e => {
    e.preventDefault()
    if (!newName.trim()) return
    const pl = await createPlaylist(newName.trim())
    setPlaylists(prev => [pl, ...prev])
    setSelectedId(pl.id)
    setNewName('')
  }

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#0f0f0f', color: '#fff' }}>
      {/* 사이드바: 플레이리스트 목록 */}
      <div style={{ width: 220, background: '#111', borderRight: '1px solid #222', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: 16, borderBottom: '1px solid #222' }}>
          <div style={{ fontWeight: 700, fontSize: 18, marginBottom: 12 }}>youflow</div>
          <form onSubmit={handleCreate} style={{ display: 'flex', gap: 6 }}>
            <input value={newName} onChange={e => setNewName(e.target.value)}
              placeholder="새 플레이리스트"
              style={{ flex: 1, background: '#222', border: '1px solid #444', color: '#fff', padding: '4px 8px', borderRadius: 4, fontSize: 12 }} />
            <button type="submit" style={{ fontSize: 12 }}>+</button>
          </form>
        </div>
        <div style={{ overflowY: 'auto', flex: 1 }}>
          {playlists.map(pl => (
            <div key={pl.id} onClick={() => setSelectedId(pl.id)}
              style={{
                padding: '10px 16px', cursor: 'pointer', fontSize: 13,
                background: pl.id === selectedId ? '#1a1a1a' : 'transparent',
                color: pl.id === selectedId ? '#fff' : '#aaa',
              }}>
              {pl.name}
            </div>
          ))}
        </div>
        <div style={{ padding: 12, borderTop: '1px solid #222' }}>
          <button onClick={() => { localStorage.removeItem('token'); navigate('/login') }}
            style={{ width: '100%', fontSize: 12, background: '#222', border: 'none', color: '#888', padding: '6px', borderRadius: 4, cursor: 'pointer' }}>
            로그아웃
          </button>
        </div>
      </div>

      {/* 메인: 영상 목록 + 재생 컨트롤 */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {selectedId
          ? <PlaylistPanel playlistId={selectedId} send={send} />
          : <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#555' }}>
              플레이리스트를 선택하세요
            </div>
        }
        <PlaybackControls send={send} />
      </div>
    </div>
  )
}
```

- [ ] **Step 5: 브라우저에서 Controller 흐름 수동 확인**

```bash
cd frontend && npm run dev
# /controller 에서 플레이리스트 생성, 영상 추가, 볼륨 슬라이더 확인
```

- [ ] **Step 6: 커밋**

```bash
git add frontend/src/
git commit -m "feat: implement Controller View with playlist and video management"
```

---

## Part 6 — Player View (React)

### Task 6-1: YouTube iframe 플레이어

**Files:**
- Create: `frontend/src/components/player/YoutubePlayer.jsx`
- Create: `frontend/src/components/player/PlayerControls.jsx`
- Create: `frontend/src/components/player/UpNextChip.jsx`
- Modify: `frontend/src/pages/PlayerPage.jsx`

- [ ] **Step 1: YouTube iframe API 로더**

`frontend/src/components/player/YoutubePlayer.jsx`:
```jsx
import { useEffect, useRef } from 'react'

function loadYouTubeAPI() {
  if (window.YT) return Promise.resolve()
  return new Promise(resolve => {
    const tag = document.createElement('script')
    tag.src = 'https://www.youtube.com/iframe_api'
    document.head.appendChild(tag)
    window.onYouTubeIframeAPIReady = resolve
  })
}

export default function YoutubePlayer({ videoId, volume, playing, onEnded }) {
  const containerRef = useRef(null)
  const playerRef = useRef(null)

  useEffect(() => {
    loadYouTubeAPI().then(() => {
      if (playerRef.current) {
        playerRef.current.destroy()
      }
      if (!videoId) return

      playerRef.current = new window.YT.Player(containerRef.current, {
        videoId,
        playerVars: { autoplay: 1, controls: 0, rel: 0, modestbranding: 1 },
        events: {
          onReady: e => {
            e.target.setVolume(volume)
            if (playing) e.target.playVideo()
          },
          onStateChange: e => {
            if (e.data === window.YT.PlayerState.ENDED) onEnded?.()
          },
        },
      })
    })
    return () => {
      playerRef.current?.destroy()
      playerRef.current = null
    }
  }, [videoId])

  useEffect(() => {
    if (!playerRef.current?.setVolume) return
    playerRef.current.setVolume(volume)
  }, [volume])

  useEffect(() => {
    if (!playerRef.current) return
    playing ? playerRef.current.playVideo?.() : playerRef.current.pauseVideo?.()
  }, [playing])

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
}
```

- [ ] **Step 2: UpNextChip 컴포넌트**

`frontend/src/components/player/UpNextChip.jsx`:
```jsx
export default function UpNextChip({ video }) {
  if (!video) return null
  return (
    <div style={{
      position: 'absolute', top: 20, right: 20,
      background: 'rgba(0,0,0,0.7)',
      border: '1px solid rgba(255,255,255,0.15)',
      backdropFilter: 'blur(8px)',
      borderRadius: 8, padding: '8px 12px',
      display: 'flex', alignItems: 'center', gap: 10,
      color: '#fff',
    }}>
      {video.thumbnail && <img src={video.thumbnail} alt="" style={{ width: 64, height: 36, objectFit: 'cover', borderRadius: 4 }} />}
      <div>
        <div style={{ fontSize: 10, color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>다음 영상</div>
        <div style={{ fontSize: 12, fontWeight: 500, maxWidth: 160 }}>{video.title}</div>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: PlayerControls 컴포넌트**

`frontend/src/components/player/PlayerControls.jsx`:
```jsx
import { useRoomStore } from '../../store/roomStore'

export default function PlayerControls({ send }) {
  const { currentVideo, playing, currentIndex, playlist } = useRoomStore()

  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      background: 'linear-gradient(to top, rgba(0,0,0,0.85) 0%, transparent 100%)',
      padding: '48px 24px 20px',
    }}>
      <div style={{ color: '#fff', fontSize: 15, fontWeight: 600, marginBottom: 12, textShadow: '0 1px 3px rgba(0,0,0,0.8)' }}>
        {currentVideo?.title}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <button onClick={() => send('/prev')} disabled={currentIndex === 0}
          style={{ background: 'none', border: 'none', color: '#fff', fontSize: 22, cursor: 'pointer', opacity: currentIndex === 0 ? 0.3 : 0.9 }}>⏮</button>
        <button onClick={() => send(playing ? '/pause' : '/play')}
          style={{ background: 'none', border: 'none', color: '#fff', fontSize: 28, cursor: 'pointer' }}>
          {playing ? '⏸' : '▶'}
        </button>
        <button onClick={() => send('/next')} disabled={currentIndex >= playlist.length - 1}
          style={{ background: 'none', border: 'none', color: '#fff', fontSize: 22, cursor: 'pointer', opacity: currentIndex >= playlist.length - 1 ? 0.3 : 0.9 }}>⏭</button>
        <span style={{ fontSize: 13, color: 'rgba(255,255,255,0.7)' }}>
          {currentIndex + 1} / {playlist.length}
        </span>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: PlayerPage 조립**

`frontend/src/pages/PlayerPage.jsx`:
```jsx
import { useState } from 'react'
import { useRoomStore } from '../store/roomStore'
import { useWebSocket } from '../hooks/useWebSocket'
import YoutubePlayer from '../components/player/YoutubePlayer'
import PlayerControls from '../components/player/PlayerControls'
import UpNextChip from '../components/player/UpNextChip'

export default function PlayerPage() {
  const [hover, setHover] = useState(false)
  const { currentVideo, playing, currentIndex, playlist } = useRoomStore()
  const { send } = useWebSocket()

  const nextVideo = playlist[currentIndex + 1] ?? null

  const handleEnded = () => send('/next')

  return (
    <div
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{ position: 'relative', width: '100vw', height: '100vh', background: '#000', overflow: 'hidden', cursor: hover ? 'default' : 'none' }}
    >
      {currentVideo
        ? <YoutubePlayer
            videoId={currentVideo.youtubeId}
            volume={currentVideo.volume}
            playing={playing}
            onEnded={handleEnded}
          />
        : <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#555', fontSize: 16 }}>
            컨트롤러에서 플레이리스트를 선택하고 재생을 시작하세요.
          </div>
      }

      {hover && (
        <>
          <UpNextChip video={nextVideo} />
          <PlayerControls send={send} />
        </>
      )}
    </div>
  )
}
```

- [ ] **Step 5: 브라우저에서 전체 흐름 수동 확인**

```bash
# 터미널 1: 백엔드
cd backend && ./gradlew bootRun

# 터미널 2: 프론트엔드
cd frontend && npm run dev

# 1. /register 에서 계정 생성
# 2. /controller 탭에서 플레이리스트 생성 → YouTube URL 추가 → 재생 버튼
# 3. /player 를 새 탭으로 열어 서브모니터로 드래그 → 영상 재생 확인
# 4. controller에서 볼륨 조절 → player에 즉시 반영 확인
```

- [ ] **Step 6: 최종 커밋**

```bash
git add frontend/src/
git commit -m "feat: implement Player View with YouTube iframe and real-time sync"
```

---

## 자기 검토 (Spec Self-Review)

### 스펙 커버리지

| 요구사항 | 구현 태스크 |
|---|---|
| 플레이리스트 URL 단건 추가 | Task 3-2 (VideoController POST /videos) |
| YouTube 플레이리스트 일괄 import | Task 3-2 (VideoController POST /import) |
| 영상별 볼륨 저장 | Task 3-2 (PATCH /volume) + Task 5-2 VideoItem |
| 서브모니터 풀스크린 재생 | Task 6-1 PlayerPage |
| WebSocket 실시간 동기화 | Task 4-1 WebSocketController |
| 로그인 기반 룸 연결 | Task 2-2 Auth + Task 4-1 RoomManager |
| 다음 영상 칩 | Task 6-1 UpNextChip |
| 영상 순서 변경 | Task 3-2 PATCH /reorder |

### 타입 일관성

- `VideoResponse` — Task 3-2에서 정의, Task 4-1 WebSocketController에서 동일하게 사용
- `StateUpdate` — Task 4-1에서 정의, Task 5-1 useWebSocket 훅에서 `setStateUpdate`로 수신
- `send('/load-playlist', { playlistId })` — Task 5-2에서 호출, Task 4-1 `@MessageMapping("/load-playlist")`에서 수신

모든 타입 및 메서드명 일관성 확인 완료.
