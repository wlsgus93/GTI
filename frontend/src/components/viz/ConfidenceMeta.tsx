import { Badge } from "@/components/ui/Badge";
import { fmtRelative } from "@/lib/format";

/**
 * 페이지 상단 데이터 신뢰도/신선도 메타 패널.
 *
 * 룰 정합:
 * - `90-data-analyst-persona.mdc` §1 — Fact / Range / Estimate 등급 명시
 * - `90-data-analyst-persona.mdc` §6 — 신선도 가드 (capturedAt 기반)
 * - `web/design-quality.md` — 데이터 정직성 = 차별화 자산 시각화
 *
 * "지금 보고 있는 화면이 어느 시점 데이터인지" 한 번에 보여줌.
 */

export type SourceMeta = {
  /** 소스 이름 (예: "Steam", "Twitch", "SteamSpy") */
  source: string;
  /** 데이터 등급 */
  grade: "Fact" | "Range" | "Estimate";
  /** 마지막 수집 시각 (ISO) — null = 미수집 */
  capturedAt: string | null;
};

type Props = {
  /** 종합 신뢰도 (선택 — 표시할 게 있으면) */
  confidence?: "HIGH" | "MEDIUM" | "LOW" | null;
  /** 데이터 시점 한 줄 (선택) */
  generatedAt?: string;
  /** 사용된 소스 메타 (Fact/Range/Estimate 분류) */
  sources?: SourceMeta[];
  /** 분석 방법 (선택 — 예: "Z-score 정규화 + 가중합") */
  method?: string;
};

const GRADE_TONE = {
  Fact: "confidence-high",
  Range: "confidence-med",
  Estimate: "stale",
} as const;

export function ConfidenceMeta({ confidence, generatedAt, sources, method }: Props) {
  return (
    <aside
      aria-label="데이터 신뢰도 메타"
      className="flex flex-wrap items-center gap-x-3 gap-y-2 rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-sunken)] px-3 py-2 text-[var(--text-meta)]"
    >
      {confidence ? (
        <span className="flex items-center gap-1.5">
          <span className="font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
            신뢰도
          </span>
          <Badge
            tone={
              confidence === "HIGH"
                ? "confidence-high"
                : confidence === "MEDIUM"
                  ? "confidence-med"
                  : "confidence-low"
            }
            dot
          >
            {confidence}
          </Badge>
        </span>
      ) : null}

      {sources && sources.length > 0 ? (
        <span className="flex items-center gap-1.5">
          <span className="font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
            소스
          </span>
          {sources.map((s) => (
            <Badge key={s.source} tone={GRADE_TONE[s.grade]} mono>
              {s.source} · {s.grade}
              {s.capturedAt ? ` · ${fmtRelative(s.capturedAt)}` : " · 미수집"}
            </Badge>
          ))}
        </span>
      ) : null}

      {method ? (
        <span className="flex items-center gap-1.5">
          <span className="font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
            방법
          </span>
          <span className="font-data text-[var(--color-ink)]">{method}</span>
        </span>
      ) : null}

      {generatedAt ? (
        <span className="ml-auto text-[var(--color-ink-subtle)]">
          분석 시점 {fmtRelative(generatedAt)}
        </span>
      ) : null}
    </aside>
  );
}
