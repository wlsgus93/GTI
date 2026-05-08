# GameTrend-Insight (GTI)

**검은토끼흰토끼**를 위한 에이전틱 게임 시장 분석 플랫폼.

9종 데이터 소스(Steam · SteamDB · SteamSpy · Twitch · YouTube · OpenCritic · IGDB · Reddit · Play/AppStore)를 동시 인입하여 7개 차원으로 분석. AI 에이전트 기반 UX로 게임 제작자의 의사결정을 지원.

> **철학**: "단순 검색은 시대역행이다." — 검색창 대신 온보딩으로 의도 파악 → 에이전트가 데이터 미리 준비 → 개인화 대시보드.

## 모노레포 구조

```
game-trend-insight/
├── frontend/   # Vite + React 19 + TypeScript SPA
├── backend/    # Spring Boot 3 + Java 17 (Gradle)
├── docs/       # 설계 문서 (architecture, ERD, API spec, ...)
└── .cursor/    # Cursor AI 가이드 (rules, agents, hooks)
```

## 빠른 시작

### Backend
```bash
cd backend
./gradlew bootRun
# http://localhost:8080
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

자세한 셋업/실행은 각 폴더의 `README.md`와 `docs/` 참고.

## 문서

### 핵심 설계
- [Roadmap](docs/roadmap.md) — **4주 스프린트 (W1~W4)**
- [Pages (P1~P10)](docs/pages.md) — 10개 페이지 명세
- [Analysis Dimensions](docs/analysis-dimensions.md) — 7개 분석 차원
- [Agentic UX](docs/agentic-ux.md) — 에이전트 기반 UX 설계
- [Case Teams (C1~C4)](docs/case-teams.md) — Pretotyping 검증 케이스
- [Architecture](docs/architecture.md) — 시스템 구성
- [ERD](docs/erd.md) — 도메인 엔티티 관계
- [API Spec](docs/api-spec.md) — REST API 명세
- [Data Sources](docs/data-sources.md) — 9종 외부 API 가이드

### 협업 / 메타
- [Cursor Commands](docs/cursor-commands.md) — ECC 슬래시 커맨드 사용 가이드
- [ECC vs Plain](docs/ecc-vs-plain.md) — 플레인 vs ECC 비교
- [Portfolio Cases](docs/portfolio/README.md) — STAR-D 형식 포트폴리오 케이스

## 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | Vite, React 19, TypeScript, Tailwind CSS v4, shadcn/ui, TanStack Query, React Router v7, Zustand, Recharts |
| Backend | Spring Boot 3, **Java 21 (Virtual Threads)**, Gradle (Kotlin DSL), Spring Data JPA, Spring Security + JWT, Spring AI, WebClient, Spring Batch |
| DB | PostgreSQL 16 |
| Cache | Redis |
| AI | **Claude** (Anthropic) — 텍스트 + Vision + NLP, **STT** (Whisper) |
| 외부 API | 9종 (Steam · SteamDB · SteamSpy · Twitch · YouTube · OpenCritic · IGDB · Reddit · Play/AppStore) |
| 배포 | Docker / docker-compose, Chrome Extension, REST API + OpenAPI |

## 페이지 (P1~P10)

P1 트렌드 보드 / P2 게임 상세 (6탭) / P3 게임 비교 / P4 워치리스트 / P5 Money Calc / P6 자사 리포트 / **P7 검증 모듈 ★** / P8 퍼블리셔 / P9 캠페인 매니저 / P10 트렌드 리포트.

## 4 케이스팀 (Pretotyping 검증)

C1 웹캠 표정 인식 호러 / C2 음성 외침 격투 / C3 LLM NPC 스토리 / **C4 시선 추적 퍼즐 (접근성) ★**
