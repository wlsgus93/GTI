import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";
import { useCampaignImpact, useCaseDetail, useCases } from "@/features/verification/hooks";
import type { CampaignWithMetrics } from "@/features/verification/api";
import { fmtInt, fmtRelative } from "@/lib/format";

/**
 * P7 검증 모듈 — Pretotyping 4 케이스 (C1~C4) + 자극물 + 캠페인 + 시차 상관 분석.
 *
 * 룰 정합:
 * - `45-pretotyping.mdc` — Pretotyping 룰
 * - `90-data-analyst-persona.mdc` — `marketer` 페르소나 핵심
 * - W7 D2 시차 상관 분석 (`/campaigns/{id}/impact`) 시각화
 */
export function VerificationPage() {
  const cases = useCases();
  const [activeCode, setActiveCode] = useState<string | undefined>(undefined);
  const detail = useCaseDetail(activeCode);

  const list = cases.data ?? [];
  const selected = activeCode ?? list[0]?.code;

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P7 Pretotyping ★ marketer 핵심"
        title="가설 검증 모듈"
        subtitle="C1~C4 케이스 · 자극물 · 캠페인 KPI · 시차 상관 분석 (캠페인 → CCU 인과)"
        trailing={
          cases.data ? (
            <Badge tone="accent" mono>
              {cases.data.length} 케이스
            </Badge>
          ) : null
        }
      />

      {cases.isLoading ? (
        <Loading label="검증 케이스 불러오는 중…" />
      ) : cases.isError ? (
        <ErrorBox error={cases.error} onRetry={() => cases.refetch()} />
      ) : list.length === 0 ? (
        <Empty label="등록된 케이스가 없습니다" />
      ) : (
        <>
          <div className="flex flex-wrap gap-2">
            {list.map((c) => {
              const active = c.code === selected;
              return (
                <button
                  key={c.code}
                  type="button"
                  onClick={() => setActiveCode(c.code)}
                  className={`inline-flex items-center gap-1 rounded-[var(--radius-input)] border px-3 py-1.5 text-sm font-medium transition ${
                    active
                      ? "border-transparent bg-[var(--color-accent)] text-white"
                      : "border-[var(--color-line)] text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
                  }`}
                >
                  <span className="font-data">{c.code}</span>
                  <span>· {c.title}</span>
                  {c.priority ? <span className="text-[var(--color-marketer)]">★</span> : null}
                </button>
              );
            })}
          </div>

          {detail.isLoading ? (
            <Loading label="케이스 상세 불러오는 중…" />
          ) : detail.isError ? (
            <ErrorBox error={detail.error} onRetry={() => detail.refetch()} />
          ) : detail.data ? (
            <CaseDetailView detail={detail.data} />
          ) : null}
        </>
      )}
    </div>
  );
}

function CaseDetailView({ detail }: { detail: NonNullable<ReturnType<typeof useCaseDetail>["data"]> }) {
  return (
    <div className="space-y-6">
      <Card variant="raised">
        <header className="flex items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-[var(--color-ink)]">{detail.title}</h2>
            <p className="mt-1 text-[var(--text-meta)] text-[var(--color-ink-muted)]">
              <span className="font-data">{detail.code}</span> · 페르소나{" "}
              {detail.targetPersona ?? "—"} · 갱신 {fmtRelative(detail.updatedAt)}
            </p>
          </div>
          <Badge
            tone={detail.status === "DONE" ? "confidence-high" : detail.status === "RUNNING" ? "fresh" : "neutral"}
            dot
          >
            {detail.status}
          </Badge>
        </header>
        {detail.hypothesis ? (
          <p className="mt-4 text-sm leading-relaxed text-[var(--color-ink)]">
            <span className="font-semibold">가설:</span> {detail.hypothesis}
          </p>
        ) : null}
        {detail.concept ? (
          <p className="mt-2 text-sm leading-relaxed text-[var(--color-ink-muted)]">
            <span className="font-semibold">컨셉:</span> {detail.concept}
          </p>
        ) : null}
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card variant="raised">
          <h3 className="font-semibold text-[var(--color-ink)]">자극물 ({detail.stimuli.length})</h3>
          {detail.stimuli.length === 0 ? (
            <div className="mt-3">
              <Empty label="등록된 자극물 없음" />
            </div>
          ) : (
            <ul className="mt-3 space-y-2">
              {detail.stimuli.map((s) => (
                <li
                  key={s.id}
                  className="rounded-[var(--radius-input)] border border-[var(--color-line)] p-3"
                >
                  <p className="font-medium text-[var(--color-ink)]">{s.title}</p>
                  <p className="mt-0.5 text-[var(--text-meta)] text-[var(--color-ink-muted)] font-data">
                    {s.type}
                  </p>
                  {s.url ? (
                    <a
                      href={s.url}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-1 inline-block text-[var(--text-meta)] text-[var(--color-accent-strong)] underline"
                    >
                      자극물 열기 ↗
                    </a>
                  ) : null}
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card variant="raised">
          <h3 className="font-semibold text-[var(--color-ink)]">캠페인 ({detail.campaigns.length})</h3>
          {detail.campaigns.length === 0 ? (
            <div className="mt-3">
              <Empty label="진행 중 캠페인 없음" />
            </div>
          ) : (
            <ul className="mt-3 space-y-3">
              {detail.campaigns.map((c) => (
                <CampaignRow key={c.id} campaign={c} />
              ))}
            </ul>
          )}
        </Card>
      </div>
    </div>
  );
}

function CampaignRow({ campaign }: { campaign: CampaignWithMetrics }) {
  const [analyzing, setAnalyzing] = useState(false);
  const [gameId, setGameId] = useState("730");
  const impact = useCampaignImpact(
    campaign.id,
    Number(gameId) || undefined,
    14,
    analyzing,
  );

  return (
    <li className="rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-sunken)] p-3">
      <div className="flex items-center justify-between gap-2">
        <p className="font-medium text-[var(--color-ink)]">{campaign.name}</p>
        <Badge
          tone={
            campaign.status === "RUNNING"
              ? "fresh"
              : campaign.status === "ENDED" || campaign.status === "DONE"
                ? "neutral"
                : "stale"
          }
          dot
        >
          {campaign.platform} · {campaign.status}
        </Badge>
      </div>
      <p className="mt-1 text-[var(--text-meta)] text-[var(--color-ink-muted)] font-data">
        UTM {campaign.utmCampaign}
      </p>
      <dl className="mt-3 grid grid-cols-3 gap-2">
        <Stat label="노출" value={fmtInt(campaign.totalImpressions)} size="sm" />
        <Stat label="클릭" value={fmtInt(campaign.totalClicks)} size="sm" />
        <Stat label="전환" value={fmtInt(campaign.totalConversions)} size="sm" />
        <Stat label="CTR" value={campaign.ctr === null ? "—" : `${(campaign.ctr * 100).toFixed(2)}%`} size="sm" />
        <Stat label="CVR" value={campaign.cvr === null ? "—" : `${(campaign.cvr * 100).toFixed(2)}%`} size="sm" />
        <Stat label="CPC" value={campaign.cpcCents === null ? "—" : `$${(campaign.cpcCents / 100).toFixed(2)}`} size="sm" />
      </dl>

      <div className="mt-3 border-t border-[var(--color-line)] pt-3">
        <div className="flex flex-wrap items-center gap-2">
          <label className="flex items-center gap-2 text-[var(--text-meta)] text-[var(--color-ink-muted)]">
            CCU 게임 ID
            <input
              type="text"
              value={gameId}
              onChange={(e) => setGameId(e.target.value)}
              className="w-20 rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] px-2 py-0.5 font-data text-sm text-[var(--color-ink)]"
            />
          </label>
          <button
            type="button"
            onClick={() => setAnalyzing(true)}
            disabled={!gameId || impact.isFetching}
            className="rounded-[var(--radius-input)] bg-[var(--color-marketer)] px-3 py-1 text-xs font-medium text-white disabled:opacity-50"
          >
            {impact.isFetching ? "분석 중…" : "시차 상관 분석"}
          </button>
          <span className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
            (캠페인 클릭 → CCU lag 0~14일)
          </span>
        </div>
        {analyzing ? (
          impact.isLoading ? (
            <div className="mt-2">
              <Loading label="Pearson 상관 계산 중…" />
            </div>
          ) : impact.isError ? (
            <div className="mt-2">
              <ErrorBox error={impact.error} />
            </div>
          ) : impact.data ? (
            <ImpactResult data={impact.data} />
          ) : null
        ) : null}
      </div>
    </li>
  );
}

function ImpactResult({
  data,
}: {
  data: NonNullable<ReturnType<typeof useCampaignImpact>["data"]>;
}) {
  const chartData = Object.entries(data.correlationsByLag)
    .map(([k, v]) => ({ lag: Number(k), r: v }))
    .sort((a, b) => a.lag - b.lag);

  return (
    <div className="mt-3 space-y-3 rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] p-3">
      <div className="flex flex-wrap items-center gap-2">
        {data.bestLagDays !== null && data.bestCorrelation !== null ? (
          <Badge tone="accent" mono>
            best lag {data.bestLagDays}d · r={data.bestCorrelation.toFixed(3)}
          </Badge>
        ) : (
          <Badge tone="stale" dot>
            sample 부족 (n &lt; 5+lag)
          </Badge>
        )}
        <Badge tone="neutral" mono>
          n={data.sampleSize}
        </Badge>
        {data.confidence ? (
          <Badge
            tone={
              data.confidence === "HIGH"
                ? "confidence-high"
                : data.confidence === "MEDIUM"
                  ? "confidence-med"
                  : "confidence-low"
            }
            dot
          >
            {data.confidence}
          </Badge>
        ) : null}
      </div>

      {chartData.length > 0 ? (
        <div className="h-[180px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-line)" />
              <XAxis dataKey="lag" tick={{ fontSize: 11, fill: "var(--color-ink-muted)" }} />
              <YAxis
                domain={[-1, 1]}
                tick={{ fontSize: 11, fill: "var(--color-ink-muted)" }}
                tickFormatter={(v) => v.toFixed(1)}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface-raised)",
                  border: "1px solid var(--color-line)",
                  borderRadius: "var(--radius-input)",
                  fontSize: 12,
                  color: "var(--color-ink)",
                }}
                formatter={(v) => [`r = ${Number(v).toFixed(3)}`, "Pearson"]}
                labelFormatter={(l) => `lag ${l}d`}
              />
              <ReferenceLine y={0} stroke="var(--color-line-strong)" />
              <Bar dataKey="r" fill="var(--color-marketer)" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : null}

      <p className="text-sm leading-relaxed text-[var(--color-ink)]">{data.interpretation}</p>
      <p className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
        ⚠️ 상관 ≠ 인과. 다른 요인 (Steam 세일/이벤트/시즌성) 통제 후 추가 검증 필요.
      </p>
    </div>
  );
}
