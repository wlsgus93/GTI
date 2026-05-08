# Frontend

Vite + React 19 + TypeScript SPA. 백엔드(`backend/`)의 REST API를 호출하는 클라이언트.

## 스택
- **빌드**: Vite
- **언어**: TypeScript (strict)
- **UI**: React 19 + Tailwind CSS v4 (프로토타입은 순수 Tailwind; shadcn는 후속 도입)
- **서버 상태**: TanStack Query v5
- **클라이언트 상태**: Zustand (필요시)
- **라우팅**: React Router v7
- **차트**: Recharts
- **HTTP**: fetch + 얇은 wrapper

## 디렉토리
```
frontend/
├── src/
│   ├── auth/           # AuthContext (JWT 영속화 + Provider)
│   ├── components/     # AgentPanel, GameCard, InsightLine, AsyncState
│   ├── features/       # 도메인별 api.ts + hooks.ts
│   │   ├── auth/       # 로그인/회원가입 (JWT)
│   │   ├── trend/      # P1 트렌드 보드
│   │   ├── game/       # P2 게임 상세 (detail/ccu/players)
│   │   ├── insight/    # P2 AI 인사이트 (multi-persona)
│   │   ├── economics/  # P2 매출/단가
│   │   ├── compare/    # P3 게임 비교
│   │   ├── watchlist/  # P4 워치리스트 (인증 필요)
│   │   ├── moneycalc/  # P5 의사결정 시뮬
│   │   └── verification/ # P7 Pretotyping
│   ├── lib/
│   │   ├── api/        # client/error/token wrapper
│   │   ├── format.ts   # 천단위/압축/USD/상대시각 포맷터
│   │   └── mock/       # publishers (P8 placeholder)
│   ├── layouts/        # MainLayout, WorkspaceLayout
│   ├── pages/          # P1~P10 페이지
│   ├── router.tsx
│   └── main.tsx
├── index.html
├── package.json
├── vite.config.ts
└── vitest.config.ts
```

## API 연동
- 모든 호출은 `src/lib/api/client.ts` 의 `apiRequest<T>()` 경유 → JWT Bearer 자동 부착
- 401 응답 시 저장된 토큰 자동 정리
- ProblemDetail (RFC 7807) 매핑 → `ApiError` 던짐
- TanStack Query로 서버 상태 관리 — 도메인별 hooks (`use*`) 노출

## 인증 흐름
- 로그인/회원가입 성공 → `AuthContext.setAuth()` → localStorage(`gti.token`) 저장
- 만료 시간 (`expiresAt`)으로 자동 만료 처리
- 워치리스트는 `authenticated` 가드 — 미인증 시 안내 카드 표시

네비·라우트 구조 변경 요약: [docs/navigation-ia-update.md](../docs/navigation-ia-update.md).  
핵심: `/` Game-Agent, `/discover` 발굴(P1), `/games/:id?tab=` 상세(P2), `/workspace/*` 비교·워치·Money(P3–P5), `/cpv`, Internal 구역에 P6·P7·P9·P10, `/login`·`/signup` 목업.

## 환경변수
```
VITE_API_BASE_URL=http://localhost:8080
```
`.env.local`에 작성. **시크릿/API 키 절대 두지 말 것** (모두 백엔드 경유).

## 실행
```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run preview
npm run lint
npm run test
```

## 컨벤션
`.cursor/rules/20-frontend-stack.mdc` 참고.
