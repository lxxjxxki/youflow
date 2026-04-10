# youflow — System Design Spec

**Date:** 2026-04-10  
**Status:** Draft

---

## 1. 프로젝트 개요

YouTube 영상을 플레이리스트로 관리하고, 영상마다 볼륨을 개별 저장하며, 재생은 서브모니터에 풀스크린으로 출력하고 조작은 다른 화면이나 기기에서 할 수 있는 웹 기반 시스템.

- **주요 사용처:** 카페/매장 (우선), 개인 홈/사무실 (차순)
- **배포 형태:** 설치 없이 웹사이트 접속으로 바로 사용 (SaaS)
- **참고 레퍼런스:** Spotify (계정 기반 기기 연결, UX 방향)

---

## 2. 핵심 요구사항

| 항목 | 내용 |
|---|---|
| 플레이리스트 관리 | URL 단건 추가 + YouTube 플레이리스트 URL 일괄 import |
| 볼륨 정규화 | 영상별 볼륨 레벨(0~100) 저장, 재생 시 자동 적용 |
| 재생 화면 | 서브모니터에 브라우저 탭 풀스크린으로 출력 |
| 조작 화면 | 메인모니터 또는 스마트폰에서 컨트롤 |
| 기기 연결 | 로그인 기반, 같은 계정 기기끼리 룸으로 묶임 |
| 실시간 동기화 | 플레이리스트 변경, 볼륨 변경 등 모든 설정 즉시 반영 |

---

## 3. 기술 스택

| 레이어 | 기술 |
|---|---|
| 프론트엔드 | React |
| 백엔드 | Spring Boot (Java) |
| 데이터베이스 | SQL (PostgreSQL 권장) |
| 실시간 통신 | WebSocket |
| YouTube 재생 | YouTube iframe API |
| YouTube 메타데이터 | YouTube Data API v3 |
| 인증 | JWT |

---

## 4. 시스템 아키텍처

```
[서브모니터]                    [메인모니터 / 스마트폰]
Player View                     Controller View
(React - 풀스크린)               (React)
YouTube iframe 재생              플레이리스트, 볼륨, 조작
        │                               │
        └──────── WebSocket ────────────┘
                       │
              Spring Boot 서버
              ┌──────────────────────┐
              │ REST API (인증/CRUD) │
              │ WebSocket 서버       │
              │ YouTube Data API     │
              └──────────────────────┘
                       │
                    SQL DB
        (계정 / 플레이리스트 / 영상 / 볼륨)
```

---

## 5. 핵심 화면 구성

### Player View (서브모니터)
- YouTube iframe 풀스크린 재생
- 마우스 호버 시에만 컨트롤 오버레이 표시
- 컨트롤: 이전 / 재생·정지 / 다음, 볼륨 슬라이더, 프로그레스 바
- 우측 상단: 다음 영상 칩 (썸네일 + 제목)
- 서버 룸 상태를 WebSocket으로 구독, 변경 즉시 반영
- YouTube 스타일 다크 테마

### Controller View (메인모니터 / 스마트폰)
- 현재 재생 중인 영상 + 볼륨 슬라이더
- 플레이리스트 목록 (드래그로 순서 변경)
- 영상별 볼륨 표시 및 조절
- URL 단건 추가 / YouTube 플레이리스트 URL import 버튼
- 재생 컨트롤 (이전 / 재생·정지 / 다음, 셔플, 반복)

---

## 6. 룸(Room) 개념

- **계정 = 룸** (Spotify Connect 방식)
- 같은 계정으로 로그인한 모든 기기가 동일 룸에 연결
- Player View 접속 시 → 해당 기기가 "player"로 룸 등록
- Controller View 접속 시 → 해당 기기가 "controller"로 룸 등록
- 플레이리스트, 볼륨 설정 등 모든 데이터는 계정에 영구 저장
- 기기 연결 끊김 시 서버 감지, 재연결 시 룸 상태 복원

---

## 7. 데이터 모델 (초안)

| 테이블 | 주요 컬럼 |
|---|---|
| `users` | id, email, password_hash, created_at |
| `playlists` | id, user_id, name, created_at |
| `videos` | id, playlist_id, youtube_url, title, thumbnail, order, volume |
| `devices` | id, user_id, type (player/controller), session_token, last_seen |

- `videos.volume` — 영상별 볼륨값 (0~100), 재생 시 `player.setVolume()` 적용
- 재생 상태 (현재 영상, 재생/정지) — DB 저장 없이 서버 메모리 + WebSocket으로 실시간 관리

---

## 8. 주요 흐름

### 재생 흐름
1. 컨트롤러에서 영상 선택 또는 다음 버튼
2. 서버 룸 상태 업데이트 (현재 영상 인덱스 변경)
3. WebSocket으로 Player View에 `VIDEO_CHANGED` 이벤트 전송
4. Player View: YouTube iframe 로드 (1~3초)
5. `onReady` 이벤트 → 저장된 볼륨값 `setVolume()` 적용 → 자동 재생

### 볼륨 조절 흐름
1. 컨트롤러에서 볼륨 슬라이더 조작
2. REST API로 서버 DB에 해당 영상 볼륨값 저장
3. WebSocket `VOLUME_CHANGED` 이벤트로 Player View에 즉시 반영

### 기기 연결 흐름
1. 브라우저에서 로그인 → JWT 발급
2. Player View 또는 Controller View 접속
3. JWT로 WebSocket 연결, 서버가 기기 타입 판별 및 룸 등록
4. 이후 모든 조작이 실시간 동기화

---

## 9. WebSocket 이벤트

**클라이언트 → 서버**
```
PLAY         { videoId }
PAUSE
NEXT
PREV
SET_VOLUME   { videoId, volume }
SEEK         { position }
```

**서버 → 클라이언트 (브로드캐스트)**
```
STATE_UPDATE      { currentVideo, isPlaying, volume, playlist }
VIDEO_CHANGED     { video }
VOLUME_CHANGED    { videoId, volume }
PLAYLIST_UPDATED  { playlist }
```

---

## 10. REST API 엔드포인트

```
POST   /auth/register
POST   /auth/login
GET    /playlists
POST   /playlists
POST   /playlists/{id}/videos         ← URL 단건 추가
POST   /playlists/{id}/import         ← YouTube 플레이리스트 URL 일괄 import
PATCH  /videos/{id}/volume            ← 볼륨값 저장
DELETE /videos/{id}
PATCH  /videos/reorder                ← 순서 변경
```

---

## 11. 향후 확장 고려사항

- 계정 하나에 여러 룸(지점) 구조 (카페 체인 대응)
- 스케줄 기반 자동 플레이리스트 전환
- 영상 재생 통계 (어떤 영상이 얼마나 재생됐는지)
