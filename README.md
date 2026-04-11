# youflow

YouTube playlist manager with per-video volume control and real-time multi-device sync.

## Features

- YouTube 플레이리스트 관리 (URL 단건 추가 / YouTube 플레이리스트 일괄 import)
- 영상별 볼륨 개별 저장 및 자동 적용
- 서브모니터에 풀스크린 플레이어 출력
- 다른 기기(스마트폰 등)에서 실시간 조작
- WebSocket 기반 실시간 동기화
- 로그인 기반 기기 연결 (Spotify Connect 방식)

## Tech Stack

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring Security, WebSocket (STOMP) |
| Database | PostgreSQL, Flyway |
| Frontend | React 18, Vite, Zustand, YouTube iframe API |
| Auth | JWT |

## Architecture

```
[Player View]          [Controller View]
서브모니터 풀스크린         메인모니터 / 스마트폰
YouTube iframe        플레이리스트, 볼륨, 조작
      │                        │
      └────── WebSocket ───────┘
                   │
          Spring Boot 서버
                   │
              PostgreSQL
```

## Built With

Designed and developed with [Claude Code](https://claude.ai/code) (Superpowers)
