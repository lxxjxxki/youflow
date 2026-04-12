package com.youflow.video;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * YouTubeApiClient 단위 테스트.
 *
 * 테스트 전략:
 *   - MockRestServiceServer로 실제 YouTube API 호출 없이 HTTP 응답을 가짜로 주입한다.
 *   - RestTemplate이 package-private 필드이므로 ReflectionTestUtils로 교체한다.
 *
 * 커버 범위:
 *   - medium 썸네일이 있는 정상 응답
 *   - medium 썸네일 없을 때 default로 폴백
 *   - items가 빈 배열 → 404
 *   - HTTP 500 서버 오류 → 502
 */
class YouTubeApiClientTest {

    private YouTubeApiClient client;
    private MockRestServiceServer mockServer;

    private static final String API_URL =
            "https://www.googleapis.com/youtube/v3/videos?part=snippet&id={videoId}&key={apiKey}";

    @BeforeEach
    void setUp() {
        client = new YouTubeApiClient();
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");

        // RestTemplate을 MockRestServiceServer로 교체하여 외부 HTTP 호출 차단
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
    }

    // ═══════════════════════════════════════════════
    // 정상 케이스
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("fetchMeta — medium 썸네일 정상 응답")
    void fetchMeta_success_withMediumThumbnail() {
        String json = """
                {
                  "items": [{
                    "snippet": {
                      "title": "Test Video Title",
                      "thumbnails": {
                        "default": { "url": "https://img.youtube.com/vi/abc/default.jpg" },
                        "medium":  { "url": "https://img.youtube.com/vi/abc/mqdefault.jpg" }
                      }
                    }
                  }]
                }
                """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        YouTubeApiClient.VideoMeta meta = client.fetchMeta("abcdefghijk");

        assertThat(meta.title()).isEqualTo("Test Video Title");
        // medium 썸네일이 있으면 medium을 사용해야 한다
        assertThat(meta.thumbnail()).isEqualTo("https://img.youtube.com/vi/abc/mqdefault.jpg");
    }

    // ═══════════════════════════════════════════════
    // 썸네일 폴백 (핵심 버그 픽스 검증)
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("fetchMeta — medium 썸네일 없을 때 default로 폴백 (구형 영상, 라이브 등)")
    void fetchMeta_fallbackToDefaultThumbnail_whenMediumMissing() {
        // medium 키가 아예 없는 응답 — 수정 전에는 NPE → 502 발생
        String json = """
                {
                  "items": [{
                    "snippet": {
                      "title": "Old Video Without Medium Thumbnail",
                      "thumbnails": {
                        "default": { "url": "https://img.youtube.com/vi/abc/default.jpg" }
                      }
                    }
                  }]
                }
                """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        YouTubeApiClient.VideoMeta meta = client.fetchMeta("abcdefghijk");

        assertThat(meta.title()).isEqualTo("Old Video Without Medium Thumbnail");
        // medium 없으면 default로 폴백되어야 한다 (수정 전: NPE)
        assertThat(meta.thumbnail()).isEqualTo("https://img.youtube.com/vi/abc/default.jpg");
    }

    @Test
    @DisplayName("fetchMeta — medium·default 모두 없을 때 빈 문자열 반환 (크래시 없음)")
    void fetchMeta_returnsEmptyString_whenNoThumbnailsAtAll() {
        String json = """
                {
                  "items": [{
                    "snippet": {
                      "title": "No Thumbnail Video",
                      "thumbnails": {}
                    }
                  }]
                }
                """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        YouTubeApiClient.VideoMeta meta = client.fetchMeta("abcdefghijk");

        assertThat(meta.title()).isEqualTo("No Thumbnail Video");
        // 썸네일이 전혀 없어도 빈 문자열을 반환하고 예외를 던지지 않아야 한다
        assertThat(meta.thumbnail()).isEmpty();
    }

    // ═══════════════════════════════════════════════
    // 에러 케이스
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("fetchMeta — items 빈 배열이면 404 예외 (YouTube에 없는 영상)")
    void fetchMeta_videoNotFound_throws404() {
        String json = """
                { "items": [] }
                """;

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetchMeta("nonExistentId"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("fetchMeta — YouTube API 서버 500 오류면 502 예외")
    void fetchMeta_youtubeServerError_throws502() {
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.fetchMeta("anyVideoId"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502");
    }
}
