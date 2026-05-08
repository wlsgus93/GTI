import { Link } from "react-router";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";

const TARGETS = [
  { id: "internal-1", title: "사내 프로젝트 A", ccu: "12.4k", wishlistDelta: "+8%" },
  { id: "internal-2", title: "사내 프로젝트 B", ccu: "3.1k", wishlistDelta: "+2%" },
];

export function InternalReportPage() {
  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P6 자사 리포트"
        title="검은토끼흰토끼 사내 타겟"
        subtitle="포트폴리오 게임 정밀 추적 — 경쟁작 알림 + 위시리스트 전환"
        trailing={
          <Badge tone="warning" dot>
            Internal
          </Badge>
        }
      />

      <p className="text-sm text-[var(--color-ink-muted)]">
        (목업) 백엔드 endpoint 미연동 — 실 데이터는 ingestion 잡 + 자사 게임 시드 추가 후 노출.
      </p>

      <div className="grid gap-4 md:grid-cols-3">
        <Card variant="raised">
          <Stat label="실시간 CCU 합산" value="15.5k" tone="accent" />
        </Card>
        <Card variant="raised">
          <Stat label="소셜 멘션 (주)" value="8.4k" />
        </Card>
        <Card variant="raised">
          <Stat label="경쟁작 알림" value="2종" hint="유사 장르 출시 예정" mono={false} />
        </Card>
      </div>

      <Card variant="raised" className="!p-0 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-[var(--color-line)] text-[var(--color-ink-muted)]">
              <th className="p-3 font-medium">타겟</th>
              <th className="p-3 font-medium">CCU</th>
              <th className="p-3 font-medium">위시리스트</th>
              <th className="p-3 font-medium">액션</th>
            </tr>
          </thead>
          <tbody>
            {TARGETS.map((t) => (
              <tr key={t.id} className="border-b border-[var(--color-line)] last:border-0">
                <td className="p-3 font-medium text-[var(--color-ink)]">{t.title}</td>
                <td className="p-3 font-data text-[var(--color-ink-muted)]">{t.ccu}</td>
                <td className="p-3 font-data text-[var(--color-confidence-high)]">{t.wishlistDelta}</td>
                <td className="p-3">
                  <Link
                    to="/games/730"
                    className="text-[var(--text-meta)] font-medium text-[var(--color-accent-strong)] underline"
                  >
                    상세 →
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
