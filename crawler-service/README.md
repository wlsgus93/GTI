# GTI Crawler Service

GTI 본체(Spring)에서 호출하지 않는 **비공식 데이터 소스** 크롤링 마이크로서비스.

## 왜 별도 서비스인가

- **격리**: Spring 본체와 ToS / Cloudflare / 봇 감지 위험 분리
- **언어 fit**: Google Play 는 `google-play-scraper` (Node.js 만), SteamDB 는 Playwright (Python/Node)
- **확장**: 향후 SteamDB / 디시 RSS 등 한 서비스로 통합

## 현재 endpoint

| Method | Path | 용도 |
|---|---|---|
| GET | `/health` | 헬스체크 |
| GET | `/charts/google-play?country=us&limit=50&collection=TOP_FREE` | Google Play 게임 차트 |

## 로컬 실행

```bash
cd crawler-service
npm install
npm start    # http://localhost:3001
```

## docker-compose

루트 `docker-compose.yml` 의 `crawler-service` 항목 참조. Spring 본체에서:
```yaml
gti.external.crawler-service.base-url: http://crawler-service:3001
```

## 향후 추가

- SteamDB Playwright (W7+ — Cloudflare stealth + 24h 캐시)
- 디시인사이드 / 인벤 RSS (W7+ — Reddit C 옵션)
