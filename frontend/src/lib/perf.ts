import { onCLS, onINP, onLCP, onFCP, onTTFB, type Metric } from "web-vitals";

/**
 * Core Web Vitals 런타임 측정.
 *
 * - **개발**: console에 출력 (Iter 3 베이스라인)
 * - **운영**: 외부 RUM (Sentry / DataDog) 으로 전송 가능 (W4+ 후속)
 *
 * 측정 단위:
 * - LCP (Largest Contentful Paint): 가장 큰 콘텐츠 표시 — 목표 < 2.5s
 * - INP (Interaction to Next Paint): 입력 반응성 — 목표 < 200ms
 * - CLS (Cumulative Layout Shift): 레이아웃 이동 — 목표 < 0.1
 * - FCP (First Contentful Paint): 첫 콘텐츠 표시
 * - TTFB (Time to First Byte): 서버 응답
 */
export function reportWebVitals(): void {
  const log = (metric: Metric) => {
    const color = metric.rating === "good" ? "color: #059669" : metric.rating === "needs-improvement" ? "color: #d97706" : "color: #dc2626";
    // eslint-disable-next-line no-console
    console.log(
      `%c[Web Vitals] ${metric.name} = ${metric.value.toFixed(1)}ms (${metric.rating})`,
      color,
    );
  };
  onLCP(log);
  onINP(log);
  onCLS(log);
  onFCP(log);
  onTTFB(log);
}
