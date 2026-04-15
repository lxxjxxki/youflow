package com.rawr.auth;

import com.rawr.user.OAuthProvider;
import com.rawr.user.Role;
import com.rawr.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-secret-key-for-testing-only-minimum-256-bits",
                86400000L
        );
    }

    private User userWithId() {
        User user = new User("test@test.com", "Tester", null, OAuthProvider.GOOGLE, "g1");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    @DisplayName("유저 정보로 JWT를 생성할 수 있다")
    void generate_createsValidToken() {
        User user = userWithId();
        String token = jwtUtil.generate(user);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("생성된 JWT는 유효하다")
    void isValid_returnsTrueForValidToken() {
        User user = userWithId();
        String token = jwtUtil.generate(user);
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("잘못된 JWT는 유효하지 않다")
    void isValid_returnsFalseForInvalidToken() {
        assertThat(jwtUtil.isValid("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("빈 문자열은 유효하지 않다")
    void isValid_returnsFalseForEmptyString() {
        assertThat(jwtUtil.isValid("")).isFalse();
    }

    @Test
    @DisplayName("JWT에서 role 클레임을 파싱할 수 있다")
    void parse_extractsRoleClaim() {
        User user = userWithId();
        String token = jwtUtil.generate(user);
        var claims = jwtUtil.parse(token);
        assertThat(claims.get("role", String.class)).isEqualTo(Role.READER.name());
    }

    @Test
    @DisplayName("JWT에서 email 클레임을 파싱할 수 있다")
    void parse_extractsEmailClaim() {
        User user = userWithId();
        String token = jwtUtil.generate(user);
        var claims = jwtUtil.parse(token);
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
    }
}
