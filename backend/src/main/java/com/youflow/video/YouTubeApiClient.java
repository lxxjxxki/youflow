package com.youflow.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * YouTube Data API v3 클라이언트.
 *
 * 역할: 영상 ID를 받아 YouTube API에서 title과 thumbnail을 가져온다.
 * VideoService에서 영상 추가 시 호출한다.
 *
 * API 엔드포인트:
 *   GET https://www.googleapis.com/youtube/v3/videos
 *       ?part=snippet&id={videoId}&key={apiKey}
 *
 * 응답 구조 (사용하는 필드만):
 * {
 *   "items": [{
 *     "snippet": {
 *       "title": "영상 제목",
 *       "thumbnails": {
 *         "medium": { "url": "썸네일 URL" }
 *       }
 *     }
 *   }]
 * }
 */
@Component
public class YouTubeApiClient {

    /** YouTube Data API v3 base URL */
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3/videos";

    /** application.yml의 youtube.api-key 값 주입 */
    @Value("${youtube.api-key}")
    private String apiKey;

    /**
     * Spring이 관리하는 HTTP 클라이언트.
     * 별도 Bean 없이 직접 생성 — VideoService에서 Mock으로 교체할 수 있도록
     * 이 클래스 자체를 @Component로 분리했다.
     * final을 제거해 테스트에서 MockRestServiceServer로 교체 가능하게 한다.
     */
    RestTemplate restTemplate = new RestTemplate();

    /**
     * YouTube 영상 메타데이터(제목, 썸네일)를 가져온다.
     *
     * @param videoId YouTube 영상 ID (예: dQw4w9WgXcQ)
     * @return 영상 메타데이터
     * @throws ResponseStatusException 404 — 해당 ID의 영상이 YouTube에 없을 때
     * @throws ResponseStatusException 502 — YouTube API 호출 자체가 실패했을 때
     */
    public VideoMeta fetchMeta(String videoId) {
        String url = String.format("%s?part=snippet&id=%s&key=%s", API_BASE, videoId, apiKey);

        try {
            // YouTube API 응답을 Map으로 역직렬화 (별도 응답 클래스 없이 처리)
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "YouTube API 응답 없음");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            // items가 비어있으면 해당 ID의 영상이 존재하지 않는 것
            if (items == null || items.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "YouTube에서 영상을 찾을 수 없습니다: " + videoId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");

            String title = (String) snippet.get("title");

            @SuppressWarnings("unchecked")
            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");

            // medium 썸네일이 없는 영상(일부 구형 영상, 라이브 등)은 default로 폴백.
            // medium이 null인 상태로 .get("url")을 호출하면 NPE → 502로 포장되는 버그 방지.
            @SuppressWarnings("unchecked")
            Map<String, Object> thumbEntry = (Map<String, Object>) thumbnails.get("medium");
            if (thumbEntry == null) {
                thumbEntry = (Map<String, Object>) thumbnails.get("default");
            }
            String thumbnailUrl = thumbEntry != null ? (String) thumbEntry.get("url") : "";

            return new VideoMeta(title, thumbnailUrl);

        } catch (ResponseStatusException e) {
            throw e; // 이미 처리한 예외는 그대로 전파
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "YouTube API 호출 실패: " + e.getMessage());
        }
    }

    /**
     * YouTube API에서 가져온 영상 메타데이터를 담는 내부 record.
     * VideoService에서 Video 엔티티 생성 시 사용한다.
     */
    public record VideoMeta(String title, String thumbnail) {}
}
