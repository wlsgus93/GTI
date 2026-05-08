import { Link } from "react-router";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { SAMPLE_GAME_ID } from "@/constants/routes";

const BANDS = [
  { tier: "마이크로", viewers: "< 500", cpv: "₩120–180 / 시청시간" },
  { tier: "미드", viewers: "500 – 5k", cpv: "₩45–75" },
  { tier: "매크로", viewers: "> 5k", cpv: "협의제 · 패키지" },
];

export function CpvPage() {
  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="스트리머 단가"
        title="협업 CPV 밴드표"
        subtitle="Twitch · YouTube 동시 시청 구간별 협업 단가 (참고 — 게임 컨텍스트 단가는 게임 상세 단가 탭)"
      />

      <Card variant="raised" className="!p-0 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-[var(--color-line)] text-[var(--color-ink-muted)]">
              <th className="p-3 font-medium">구간</th>
              <th className="p-3 font-medium">평균 동시 시청</th>
              <th className="p-3 font-medium">CPV</th>
            </tr>
          </thead>
          <tbody>
            {BANDS.map((b) => (
              <tr key={b.tier} className="border-b border-[var(--color-line)] last:border-0">
                <td className="p-3 font-medium text-[var(--color-ink)]">{b.tier}</td>
                <td className="p-3 font-data text-[var(--color-ink-muted)]">{b.viewers}</td>
                <td className="p-3 font-data text-[var(--color-ink)]">{b.cpv}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <p className="text-sm text-[var(--color-ink-muted)]">
        게임별 단가는{" "}
        <Link
          to={`/games/${SAMPLE_GAME_ID}?tab=cpv`}
          className="font-medium text-[var(--color-accent-strong)] underline"
        >
          게임 상세 → 단가 탭
        </Link>{" "}
        에서 확인.
      </p>
    </div>
  );
}
