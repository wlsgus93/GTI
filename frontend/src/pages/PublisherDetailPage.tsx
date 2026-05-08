import { Link, useParams } from "react-router";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { MOCK_PUBLISHERS } from "@/lib/mock/publishers";

export function PublisherDetailPage() {
  const { id } = useParams<{ id: string }>();
  const pub = MOCK_PUBLISHERS.find((p) => p.id === id) ?? MOCK_PUBLISHERS[0];

  return (
    <div className="space-y-[var(--space-section)]">
      <Link
        to="/publishers"
        className="text-[var(--text-meta)] text-[var(--color-ink-muted)] hover:underline"
      >
        ← 목록
      </Link>
      <Hero
        eyebrow="P8 퍼블리셔 상세"
        title={pub.name}
        subtitle="(목업) 포트폴리오 차트 + 장르 점유율 — 백엔드 endpoint 미구현"
      />

      <Card variant="raised">
        <p className="text-sm text-[var(--color-ink-muted)]">
          Recharts 포트폴리오 영역 (placeholder)
        </p>
      </Card>

      <Card variant="raised">
        <h2 className="font-semibold text-[var(--color-ink)]">대표 타이틀</h2>
        <ul className="mt-3 space-y-2 text-sm">
          <li>
            <Link
              to="/games/730"
              className="text-[var(--color-accent-strong)] underline"
            >
              샘플 게임 → P2 게임 상세
            </Link>
          </li>
        </ul>
      </Card>
    </div>
  );
}
