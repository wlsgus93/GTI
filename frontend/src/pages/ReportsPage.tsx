import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";

const HISTORY = [
  { id: "r1", title: "지난 주 글로벌 트렌드", created: "2026-05-01", format: "Markdown" },
  { id: "r2", title: "월간 장르 클러스터", created: "2026-04-01", format: "PDF" },
];

export function ReportsPage() {
  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P10 트렌드 리포트"
        title="LLM 자동 작성 보고서"
        subtitle="주제 + 범위 입력 → Claude 가 Evidence/Insight/Strategy 3단 구조로 작성"
        trailing={<Badge tone="warning" dot>Internal</Badge>}
      />

      <p className="text-sm text-[var(--color-ink-muted)]">
        (목업) 발행 endpoint 미연동 — Spring AI Anthropic 통합 후 활성.
      </p>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card variant="raised">
          <h2 className="font-semibold text-[var(--color-ink)]">새 리포트</h2>
          <form className="mt-4 space-y-3">
            <label className="block text-sm">
              <span className="text-[var(--color-ink-muted)]">주제</span>
              <input
                type="text"
                placeholder="예: 로그라이트 서브장르 동향"
                className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]"
              />
            </label>
            <label className="block text-sm">
              <span className="text-[var(--color-ink-muted)]">범위</span>
              <select className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]">
                <option>글로벌 Steam</option>
                <option>아시아 모바일</option>
              </select>
            </label>
            <label className="block text-sm">
              <span className="text-[var(--color-ink-muted)]">형식</span>
              <select className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-3 py-2 text-sm text-[var(--color-ink)]">
                <option>Markdown</option>
                <option>PDF</option>
              </select>
            </label>
            <button
              type="button"
              disabled
              className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
            >
              생성 (목업 — endpoint 대기)
            </button>
          </form>
        </Card>

        <Card variant="raised">
          <h2 className="font-semibold text-[var(--color-ink)]">발행 이력</h2>
          <ul className="mt-4 divide-y divide-[var(--color-line)]">
            {HISTORY.map((h) => (
              <li key={h.id} className="flex items-center justify-between py-3 text-sm">
                <span className="font-medium text-[var(--color-ink)]">{h.title}</span>
                <span className="font-data text-[var(--color-ink-muted)]">
                  {h.created} · {h.format}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  );
}
