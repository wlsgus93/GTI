// GTI Crawler Service — Google Play Charts (W7+ 별도 마이크로서비스)
//
// Spring 본체에서 직접 google-play-scraper 호출 시 ToS / Cloudflare / Java 라이브러리 부재 등
// 위험. 별도 Node.js 컨테이너로 격리하여 호출 (REST → JSON).
//
// 향후 SteamDB Playwright 도 같은 서비스에 추가 (W2 plan 통합 진행).

import express from 'express';
import gplay from 'google-play-scraper';

const app = express();
const PORT = process.env.PORT || 3001;

// 헬스체크 (docker-compose / k8s liveness 용)
app.get('/health', (_req, res) => {
  res.json({ status: 'UP', service: 'gti-crawler', time: new Date().toISOString() });
});

/**
 * Google Play Top Free Games chart.
 *
 * GET /charts/google-play?country=us&limit=50
 *
 * Response:
 * {
 *   "country": "us",
 *   "category": "GAME",
 *   "collection": "TOP_FREE",
 *   "fetchedAt": "2026-...",
 *   "items": [
 *     { "rank": 1, "appId": "com.example.game", "title": "...", "developer": "...", "score": 4.5, "free": true }
 *   ]
 * }
 */
app.get('/charts/google-play', async (req, res) => {
  const country = String(req.query.country || 'us').toLowerCase();
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 50, 1), 100);
  const collectionName = String(req.query.collection || 'TOP_FREE').toUpperCase();
  const collection = gplay.collection[collectionName] || gplay.collection.TOP_FREE;

  try {
    const data = await gplay.list({
      collection,
      category: gplay.category.GAME,
      country,
      num: limit,
    });
    const items = data.map((app, idx) => ({
      rank: idx + 1,
      appId: app.appId,
      title: app.title,
      developer: app.developer,
      developerId: app.developerId ?? null,
      score: app.score ?? null,
      free: app.free ?? true,
      icon: app.icon ?? null,
    }));
    res.json({
      country,
      category: 'GAME',
      collection: collectionName,
      fetchedAt: new Date().toISOString(),
      items,
    });
  } catch (err) {
    console.error('[gplay] fetch failed', err);
    res.status(502).json({
      error: 'upstream_failed',
      message: String(err?.message || err),
    });
  }
});

// 404
app.use((_req, res) => {
  res.status(404).json({ error: 'not_found' });
});

app.listen(PORT, () => {
  console.log(`[gti-crawler] listening on :${PORT}`);
});
