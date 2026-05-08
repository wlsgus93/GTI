import { Link } from "react-router";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";
import { MOCK_PUBLISHERS } from "@/lib/mock/publishers";

export function PublisherListPage() {
  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P8 퍼블리셔"
        title="포트폴리오 추적"
        subtitle="(목업) 백엔드 publisher endpoint 미구현 — 시드 데이터로 UI 검증"
      />

      <Card variant="raised" className="!p-0">
        <ul className="divide-y divide-[var(--color-line)]">
          {MOCK_PUBLISHERS.map((p) => (
            <li key={p.id} className="flex items-center justify-between p-4">
              <div>
                <Link
                  to={`/publishers/${p.id}`}
                  className="font-semibold text-[var(--color-ink)] hover:underline"
                >
                  {p.name}
                </Link>
                <p className="mt-0.5 text-[var(--text-meta)] text-[var(--color-ink-muted)] font-data">
                  타이틀 {p.gameCount} · 점유 {p.sharePct}%
                </p>
              </div>
              <Stat label="점유" value={`${p.sharePct}%`} size="sm" tone="accent" />
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
