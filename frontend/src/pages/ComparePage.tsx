import { useMemo } from "react";
import { useSearchParams } from "react-router";
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
} from "recharts";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { ConfidenceMeta } from "@/components/viz/ConfidenceMeta";
import { useCompare } from "@/features/compare/hooks";
import type { CompareItem } from "@/features/compare/api";
import { fmtCompact, fmtInt, fmtPct } from "@/lib/format";

/**
 * P3 게임 비교 — Virtual Threads 병렬 호출 + wallClock 시각화.
 *
 * 룰 정합:
 * - `web/design-quality.md` — radar chart 토큰 정합 + grid-breaking
 * - `90-data-analyst-persona.mdc` §1 — Confidence 등급 (per-item)
 * - 백엔드 자산 시각화: wallClockMs hero 노출 (Iter 86 FE STAR-D 트리거 D)
 */
export function ComparePage() {
  const [params] = useSearchParams();
  const ids = useMemo(
    () => (params.get("ids") ?? "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    [params],
  );

  const compare = useCompare(ids);

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P3 게임 비교"
        title="여러 게임을 한 번에"
        subtitle="Virtual Threads 로 게임마다 동시에 호출 — wallClock = max(per-game latency)"
        trailing={
          compare.data ? (
            <div className="flex flex-col items-end gap-1">
              <Badge tone="accent" mono>
                wallClock {compare.data.wallClockMs}ms
              </Badge>
              <span className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
                {compare.data.items.length}개 비교 · 미존재 {compare.data.missingGameIds.length}
              </span>
            </div>
          ) : null
        }
      />

      {ids.length < 2 ? (
        <Empty
          label="비교에는 2~5개 게임 ID 가 필요합니다"
          hint="URL에 ?ids=730,1245620 형식으로 추가하거나, 트렌드 보드의 비교 버튼을 사용하세요"
        />
      ) : compare.isLoading ? (
        <Loading label="병렬 호출 중…" />
      ) : compare.isError ? (
        <ErrorBox error={compare.error} onRetry={() => compare.refetch()} />
      ) : compare.data ? (
        <>
          <ConfidenceMeta
            confidence={null}
            generatedAt={compare.data.generatedAt}
            sources={[
              { source: "Steam", grade: "Fact", capturedAt: compare.data.generatedAt },
              { source: "SteamSpy", grade: "Range", capturedAt: compare.data.generatedAt },
              { source: "Twitch", grade: "Fact", capturedAt: compare.data.generatedAt },
            ]}
            method="병렬 호출 + 정규화 비교"
          />

          <div className="flex flex-wrap items-center gap-2">
            {compare.data.items.map((it) => (
              <Badge key={it.gameId} tone="neutral">
                {it.name}
              </Badge>
            ))}
            {compare.data.missingGameIds.length > 0 ? (
              <Badge tone="stale" dot>
                미존재 {compare.data.missingGameIds.join(", ")}
              </Badge>
            ) : null}
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card variant="raised" className="!p-4">
              <h2 className="mb-2 text-sm font-semibold text-[var(--color-ink)]">
                5축 정규화 RadarChart
              </h2>
              <p className="mb-3 text-[var(--text-meta)] text-[var(--color-ink-muted)]">
                각 축은 비교 대상 중 최대값 기준 0~100 정규화
              </p>
              <CompareRadar items={compare.data.items} />
            </Card>

            <Card variant="raised" className="!p-0 overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-[var(--color-line)] text-[var(--color-ink-muted)]">
                    <th className="p-3 font-medium">메트릭</th>
                    {compare.data.items.map((it) => (
                      <th key={it.gameId} className="p-3 font-medium text-[var(--color-ink)]">
                        {it.name}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <Row label="현재 CCU" items={compare.data.items} render={(it) => fmtInt(it.latestCcu)} mono />
                  <Row label="24h Δ%" items={compare.data.items} render={(it) => fmtPct(it.ccuDeltaPct)} mono />
                  <Row label="Twitch 시청자" items={compare.data.items} render={(it) => fmtInt(it.twitchViewers)} mono />
                  <Row label="총 멘션" items={compare.data.items} render={(it) => fmtInt(it.totalMentions)} mono />
                  <Row label="긍정 리뷰" items={compare.data.items} render={(it) => it.reviewScorePercent === null ? "—" : `${it.reviewScorePercent.toFixed(1)}%`} mono />
                  <Row label="Owners (mid)" items={compare.data.items} render={(it) => fmtCompact(it.ownersMid)} mono />
                  <Row label="가격 (USD)" items={compare.data.items} render={(it) => it.priceUsd ? `$${it.priceUsd}` : "—"} mono />
                  <Row label="개발사 net" items={compare.data.items} render={(it) => it.developerNetRevenue ? `$${fmtCompact(Number(it.developerNetRevenue))}` : "—"} mono />
                  <Row label="신뢰도" items={compare.data.items} render={(it) => it.confidence ?? "—"} mono={false} />
                </tbody>
              </table>
            </Card>
          </div>
        </>
      ) : null}
    </div>
  );
}

function CompareRadar({ items }: { items: CompareItem[] }) {
  const dims = [
    { key: "ccu", label: "동접", pick: (it: CompareItem) => it.latestCcu ?? 0 },
    { key: "viewers", label: "시청자", pick: (it: CompareItem) => it.twitchViewers ?? 0 },
    { key: "mentions", label: "멘션", pick: (it: CompareItem) => it.totalMentions ?? 0 },
    { key: "review", label: "리뷰", pick: (it: CompareItem) => it.reviewScorePercent ?? 0 },
    { key: "owners", label: "Owners", pick: (it: CompareItem) => it.ownersMid ?? 0 },
  ];
  const data = dims.map((d) => {
    const max = Math.max(1, ...items.map((it) => d.pick(it)));
    const row: Record<string, string | number> = { dimension: d.label };
    items.forEach((it, i) => {
      row[`g${i}`] = Math.round((d.pick(it) / max) * 100);
    });
    return row;
  });

  // 페르소나 accent + 보조 색상 (대비 확보)
  const colors = [
    "var(--color-accent)",
    "var(--color-publisher)",
    "var(--color-marketer)",
    "var(--color-investor)",
    "var(--color-indie)",
  ];

  return (
    <div className="h-[300px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart data={data} margin={{ top: 12, right: 24, bottom: 12, left: 24 }}>
          <PolarGrid stroke="var(--color-line)" />
          <PolarAngleAxis dataKey="dimension" tick={{ fontSize: 11, fill: "var(--color-ink-muted)" }} />
          <PolarRadiusAxis domain={[0, 100]} tick={false} axisLine={false} />
          <Tooltip
            contentStyle={{
              background: "var(--color-surface-raised)",
              border: "1px solid var(--color-line)",
              borderRadius: "var(--radius-input)",
              fontSize: 12,
              color: "var(--color-ink)",
            }}
          />
          {items.map((it, i) => (
            <Radar
              key={it.gameId}
              name={it.name}
              dataKey={`g${i}`}
              stroke={colors[i % colors.length]}
              fill={colors[i % colors.length]}
              fillOpacity={0.15}
              strokeWidth={2}
            />
          ))}
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}

type RowProps = {
  label: string;
  items: CompareItem[];
  render: (it: CompareItem) => string;
  mono: boolean;
};

function Row({ label, items, render, mono }: RowProps) {
  return (
    <tr className="border-b border-[var(--color-line)] last:border-0">
      <td className="p-3 font-medium text-[var(--color-ink-muted)]">{label}</td>
      {items.map((it) => (
        <td key={it.gameId} className={`p-3 text-[var(--color-ink)] ${mono ? "font-data" : ""}`}>
          {render(it)}
        </td>
      ))}
    </tr>
  );
}
