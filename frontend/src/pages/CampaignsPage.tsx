import { Link } from "react-router";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";

const ROWS = [
  { id: "c1", name: "Steam 위시 공략", platform: "Steam", status: "RUNNING", budget: "₩12M", spent: "₩4.2M" },
  { id: "c2", name: "검증 캠페인 B", platform: "Meta", status: "DONE", budget: "₩8M", spent: "₩7.8M" },
  { id: "c3", name: "인플루언서 예고", platform: "YouTube", status: "SCHEDULED", budget: "₩20M", spent: "₩0" },
];

export function CampaignsPage() {
  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P9 캠페인 매니저"
        title="마케팅 + 검증 캠페인 통합"
        subtitle="P7 검증 모듈 캠페인과 통합 시각화 (참고 — 실 캠페인 CRUD 는 P7 에서)"
        trailing={
          <Badge tone="warning" dot>
            Internal
          </Badge>
        }
      />

      <p className="text-sm text-[var(--color-ink-muted)]">
        (목업) 실 데이터는 P7 검증 모듈 캠페인 endpoint 와 통합 예정.
      </p>

      <Card variant="raised" className="!p-0 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-[var(--color-line)] text-[var(--color-ink-muted)]">
              <th className="p-3 font-medium">이름</th>
              <th className="p-3 font-medium">플랫폼</th>
              <th className="p-3 font-medium">상태</th>
              <th className="p-3 font-medium">예산</th>
              <th className="p-3 font-medium">사용</th>
              <th className="p-3 font-medium">링크</th>
            </tr>
          </thead>
          <tbody>
            {ROWS.map((r) => (
              <tr key={r.id} className="border-b border-[var(--color-line)] last:border-0">
                <td className="p-3 font-medium text-[var(--color-ink)]">{r.name}</td>
                <td className="p-3 text-[var(--color-ink-muted)]">{r.platform}</td>
                <td className="p-3">
                  <Badge
                    tone={
                      r.status === "RUNNING"
                        ? "fresh"
                        : r.status === "DONE"
                          ? "neutral"
                          : "stale"
                    }
                    dot
                  >
                    {r.status}
                  </Badge>
                </td>
                <td className="p-3 font-data text-[var(--color-ink)]">{r.budget}</td>
                <td className="p-3 font-data text-[var(--color-ink-muted)]">{r.spent}</td>
                <td className="p-3">
                  <Link
                    to="/verification"
                    className="text-[var(--text-meta)] font-medium text-[var(--color-accent-strong)] underline"
                  >
                    P7 →
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
