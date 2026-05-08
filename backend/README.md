# Backend

Spring Boot 3 + Java 21 + Gradle. 게임 데이터 수집/저장/분석 REST API 서버.

> **W1 Foundation 완료** (2026-05-06): 9 어댑터 (8 per-game + 1 카테고리), Virtual Threads 오케스트레이터, Resilience4j 회로 차단기, 일일 스케줄러, 시계열 적재.
> 진행 상황: `docs/dev-log.md`. 포트폴리오 케이스: `docs/portfolio/`.

## 스택
- **Java 21** (LTS) — record, sealed, switch expression, **Virtual Threads**
- **Spring Boot 3.4** + **Gradle Kotlin DSL** (`build.gradle.kts`)
- **PostgreSQL 16** + **Spring Data JPA + Hibernate**
- **Redis 7** (Lettuce) — 외부 API 응답 캐시 + OAuth 토큰
- **Spring Security + JWT** (W3에서 활성화 예정)
- **Spring AI Anthropic** (W2에서 활성화)
- **Spring WebClient** (Reactor Netty) — 9 외부 API 호출
- **Resilience4j** — per-source 회로 차단기 + 재시도
- **Flyway** — 스키마 마이그레이션 (V1: master, V2: snapshots)
- **springdoc-openapi** — Swagger UI 자동 생성
- **JUnit 5 + Mockito + AssertJ + Testcontainers + WireMock + JaCoCo**

## 패키지 구조 (헥사고날 영감)
```
src/main/java/com/gametrend/insight/
├── domain/           # POJO 엔티티 (Spring 의존성 X): game, snapshot, user
├── application/      # UseCase, 트랜잭션 경계
│   ├── port/out/     # 9 외부 소스 포트 인터페이스
│   └── ingestion/    # IngestionOrchestrator (Virtual Threads + sealed Result)
├── infrastructure/   # 어댑터: JPA repository, 외부 API 클라이언트, 캐시
│   ├── persistence/  # 12 JPA entities + repositories
│   └── external/
│       ├── common/   # AbstractExternalApiClient + RetryPolicy + RedisCacheTemplate + ExternalApiMetrics
│       ├── steam/, steamstore/, steamspy/, twitch/, igdb/, youtube/, reddit/, opencritic/, apple/
│       └── oauth/    # TwitchOAuthTokenProvider + RedditOAuthTokenProvider
├── presentation/     # REST 컨트롤러 + GlobalExceptionHandler
└── config/           # WebClient, Redis, VirtualThread, OpenApi, Security, Scheduling
```

## 실행 (W1 데모)

### 1. 인프라 기동
```bash
cd backend
docker compose up -d   # postgres:16-alpine + redis:7-alpine

# 컨테이너 상태 확인
docker compose ps      # postgres:5432, redis:6379 모두 healthy
```

### 2. 환경 변수 (선택, 외부 API 호출 시)
`.env.example` 복사 후 `.env`로:
```bash
cp .env.example .env
# 편집: STEAM_API_KEY, TWITCH_CLIENT_ID/SECRET, YOUTUBE_API_KEY, REDDIT_CLIENT_ID/SECRET, ANTHROPIC_API_KEY
```
또는 `application-local.yml` (gitignore됨):
```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 편집
```

### 3. 앱 실행
```bash
./gradlew bootRun
# http://localhost:8080
```

### 4. 검증
```bash
# Swagger UI
curl -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html
# → 200

# Health
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# Ping
curl http://localhost:8080/api/v1/ping
# → {"service":"gti","status":"ok","timestamp":"..."}

# Metrics — 외부 API 호출 후 확인
curl http://localhost:8080/actuator/metrics | jq '.names | map(select(startswith("external.api")))'
```

### 5. 일일 수집 잡
- **자동**: 매일 03:00 UTC (`@Scheduled` cron)
- **수동 트리거**: `DailyIngestionService.runOnce()` 호출 (REST 엔드포인트는 W2에서 노출 예정)

## 테스트

```bash
./gradlew test
# 33+ tests / 32 pass / 1 skip / 0 fail (W1 끝)

./gradlew test jacocoTestReport
# build/reports/jacoco/test/html/index.html
```

## W1 어댑터 + 회로 차단기

| 어댑터 | 인증 | 캐시 TTL | 회로 차단기 | 상태 |
|---|---|---|---|---|
| Steam Web API | API Key | 5min (CCU) | ✅ | ✅ |
| Steam Storefront | 없음 | 1h (price) | ✅ | ✅ |
| SteamSpy | 비공식 | 24h | ✅ | ✅ |
| Twitch Helix | OAuth (공유) | 5min (viewers) | ✅ | ✅ |
| IGDB | OAuth (Twitch 공유) | 24h (메타) | ✅ | ✅ |
| YouTube Data v3 | API Key | 6h | ✅ | ✅ |
| Reddit | OAuth + UA | 1h | ✅ | ✅ |
| OpenCritic | optional API Key | 24h | ✅ | ✅ |
| Apple Top Charts | 없음 | 1h (per category) | ✅ | ✅ (per-game 외) |

회로 차단기: 50% 실패율 → OPEN 30s, 5xx/timeout만 카운트, 4xx 무시.

## 실행 시 환경변수

| 변수 | 필수? | 비고 |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | yes | 기본 `jdbc:postgresql://localhost:5432/gti, gti, gti` |
| `REDIS_HOST`, `REDIS_PORT` | no | 기본 `localhost:6379` |
| `STEAM_API_KEY` | optional | 설정 시 Web API 호출에 추가 |
| `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` | yes (Twitch/IGDB 사용 시) | Twitch + IGDB 공유 |
| `YOUTUBE_API_KEY` | yes (YouTube 사용 시) | Google Cloud Console |
| `REDDIT_CLIENT_ID`, `REDDIT_CLIENT_SECRET`, `REDDIT_USER_AGENT` | yes (Reddit 사용 시) | reddit.com/prefs/apps |
| `OPENCRITIC_API_KEY` | optional | RapidAPI |
| `ANTHROPIC_API_KEY` | yes (W2 LLM 활성화 시) | Spring AI |

## 문서

- 진행 일지: `docs/dev-log.md` (Mermaid Gantt + Day별 회고)
- 포트폴리오 케이스: `docs/portfolio/`
  - `2026-05-06-reusable-adapter-pattern.md` — 9 어댑터의 공통 베이스
  - `2026-05-06-virtual-threads-orchestrator.md` — 8 소스 병렬 수집
- 룰: `.cursor/rules/`
  - `30-backend-stack.mdc` — Java 21, 헥사고날, Virtual Thread pinning 회피
  - `60-data-sources.mdc` — 9 소스 인증/레이트
  - `80-portfolio-cases.mdc` — STAR-D 트리거
- 디자인: `docs/architecture.md`, `docs/erd.md`, `docs/api-spec.md`, `docs/data-sources.md`

## 다음 (W2 진입)

- **7 차원 분석 v1** (D1~D7) — Vision LLM 포함
- **P1 트렌드 보드** + **P2 게임 상세 (6탭)** + **P3 게임 비교**
- **Game-Agent** Spring AI Anthropic 1분 요약
- 자세한 일정: `docs/roadmap.md` W2 섹션
