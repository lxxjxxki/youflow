package com.youflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youflow.auth.dto.LoginRequest;
import com.youflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
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
@ActiveProfiles("test")   // application-test.yml 사용 (youflow_test DB)
@Transactional            // 각 테스트 후 DB 롤백 → 테스트 간 데이터 격리
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    // ────────────────────────────────────────
    // 회원가입 테스트
    // ────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공 - 200 OK + JWT 토큰 반환")
    void register_success() throws Exception {
        var req = new RegisterRequest("newuser@youflow.com", "password123");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isOk())
            // token 필드가 존재하고 비어있지 않아야 함
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("회원가입 실패 - 동일 이메일 중복 가입 시 409 CONFLICT")
    void register_duplicateEmail_returns409() throws Exception {
        var req = new RegisterRequest("duplicate@youflow.com", "password123");

        // 첫 번째 가입은 성공
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isOk());

        // 같은 이메일로 두 번째 가입 시도 → 409
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 8자 미만 시 400 BAD REQUEST")
    void register_shortPassword_returns400() throws Exception {
        // @Size(min = 8) 유효성 검증에 걸려야 함
        var req = new RegisterRequest("valid@youflow.com", "short");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 형식 오류 시 400 BAD REQUEST")
    void register_invalidEmail_returns400() throws Exception {
        // @Email 유효성 검증에 걸려야 함
        var req = new RegisterRequest("not-an-email", "password123");

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    // ────────────────────────────────────────
    // 로그인 테스트
    // ────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 - 200 OK + JWT 토큰 반환")
    void login_success() throws Exception {
        // 먼저 회원가입
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(
                    new RegisterRequest("login@youflow.com", "password123"))))
            .andExpect(status().isOk());

        // 로그인 시도
        var req = new LoginRequest("login@youflow.com", "password123");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 틀렸을 때 401 UNAUTHORIZED")
    void login_wrongPassword_returns401() throws Exception {
        // 먼저 회원가입
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(
                    new RegisterRequest("wrongpw@youflow.com", "password123"))))
            .andExpect(status().isOk());

        // 틀린 비밀번호로 로그인 시도
        var req = new LoginRequest("wrongpw@youflow.com", "wrongpassword");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일로 로그인 시 401 UNAUTHORIZED")
    void login_nonExistentEmail_returns401() throws Exception {
        // 가입하지 않은 이메일로 로그인 시도
        // 보안상 "이메일이 없다"는 정보를 주지 않고 401만 반환해야 함
        var req = new LoginRequest("ghost@youflow.com", "password123");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }
}
