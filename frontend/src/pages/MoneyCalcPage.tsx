import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router";
import { ErrorBox, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";
import { ConfidenceMeta } from "@/components/viz/ConfidenceMeta";
import { useEconomics } from "@/features/economics/hooks";
import { useGameDetail } from "@/features/game/hooks";
import { useMoneyCalcSimulation } from "@/features/moneycalc/hooks";
import type { MoneyCalcRequest } from "@/features/moneycalc/api";
import { fmtCompact, fmtInt, fmtPct } from "@/lib/format";

/**
 * P5 MoneyCalc — 게임 발매 매출 시뮬레이터.
 *
 * 룰 정합:
 * - 90 §1 — Confidence (HIGH/MEDIUM/LOW) Monte Carlo 정량 출력
 * - 90 §2 — D7 가격 전략 차원 입력
 * - 백엔드 자산 시각화: profit probability + p10/50/90 + 민감도
 *
 * Economics prefill: `?gameId=` 쿼리 → 게임 상세 매출 탭에서 자동 채움.
 */

type FormState = {
  pessOwners: number;
  realOwners: number;
  optOwners: number;
  pessPriceUsd: number;
  realPriceUsd: number;
  optPriceUsd: number;
  developmentUsd: number;
  marketingUsd: number;
  iterations: number;
};

const DEFAULT_FORM: FormState = {
  pessOwners: 50_000,
  realOwners: 200_000,
  optOwners: 800_000,
  pessPriceUsd: 14.99,
  realPriceUsd: 19.99,
  optPriceUsd: 24.99,
  developmentUsd: 1_500_000,
  marketingUsd: 500_000,
  iterations: 1000,
};

const toCents = (usd: number) => Math.round(usd * 100);

function buildRequest(form: FormState): MoneyCalcRequest {
  return {
    pessimistic: { owners: form.pessOwners, priceCents: toCents(form.pessPriceUsd) },
    realistic: { owners: form.realOwners, priceCents: toCents(form.realPriceUsd) },
    optimistic: { owners: form.optOwners, priceCents: toCents(form.optPriceUsd) },
    developmentCostCents: toCents(form.developmentUsd),
    marketingCostCents: toCents(form.marketingUsd),
    monteCarloIterations: form.iterations,
  };
}

export function MoneyCalcPage() {
  const [params] = useSearchParams();
  const gameIdParam = params.get("gameId");
  const gameId = gameIdParam ? Number(gameIdParam) : undefined;

  // Economics prefill — 게임 상세 매출 탭 → "Money Calc 시뮬레이션" 링크
  const eco = useEconomics(gameId);
  const detail = useGameDetail(gameId);
  const [form, setForm] = useState<FormState>(DEFAULT_FORM);
  const [prefilled, setPrefilled] = useState(false);

  // Economics 응답이 도착하면 1회만 prefill (사용자 수정 가능)
  useEffect(() => {
    if (prefilled || !eco.data?.revenue) return;
    const r = eco.data.revenue;
    if (r.ownersMid && r.priceUsd) {
      const mid = r.ownersMid;
      const price = Number(r.priceUsd);
      setForm((f) => ({
        ...f,
        pessOwners: Math.round(mid * 0.3),
        realOwners: mid,
        optOwners: Math.round(mid * 2.5),
        pessPriceUsd: Math.round(price * 0.8 * 100) / 100,
        realPriceUsd: price,
        optPriceUsd: Math.round(price * 1.2 * 100) / 100,
      }));
      setPrefilled(true);
    }
  }, [eco.data, prefilled]);

  const sim = useMoneyCalcSimulation();

  const update = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sim.mutate(buildRequest(form));
  };

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow={detail.data ? `${detail.data.name} 기준 prefill` : "P5 MoneyCalc"}
        title="발매 매출 시뮬레이션"
        subtitle="3 시나리오 (비관/보통/낙관) + Monte Carlo + OAT 민감도. Steam 30% cut + 환불 5% 가정."
        trailing={
          gameId && detail.data ? (
            <Link
              to={`/games/${detail.data.steamAppId ?? detail.data.id}`}
              className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-3 py-1.5 text-xs font-medium text-[var(--color-ink)]"
            >
              ← 게임 상세
            </Link>
          ) : null
        }
      />

      {prefilled && eco.data ? (
        <ConfidenceMeta
          confidence={eco.data.confidence}
          generatedAt={eco.data.lastUpdated ?? undefined}
          sources={[
            { source: "SteamSpy", grade: "Range", capturedAt: eco.data.lastUpdated },
            { source: "Steam Store", grade: "Fact", capturedAt: eco.data.lastUpdated },
          ]}
          method={`Owners 범위 [${fmtCompact(eco.data.revenue?.ownersLow)}, ${fmtCompact(eco.data.revenue?.ownersHigh)}] × 0.95 × 0.70`}
        />
      ) : null}

      <form
        onSubmit={handleSubmit}
        className="space-y-4 rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] p-6"
      >
        <div className="grid gap-4 md:grid-cols-3">
          <ScenarioGroup
            title="비관"
            tone="rose"
            owners={form.pessOwners}
            price={form.pessPriceUsd}
            onOwners={(v) => update("pessOwners", v)}
            onPrice={(v) => update("pessPriceUsd", v)}
          />
          <ScenarioGroup
            title="보통"
            tone="neutral"
            owners={form.realOwners}
            price={form.realPriceUsd}
            onOwners={(v) => update("realOwners", v)}
            onPrice={(v) => update("realPriceUsd", v)}
          />
          <ScenarioGroup
            title="낙관"
            tone="emerald"
            owners={form.optOwners}
            price={form.optPriceUsd}
            onOwners={(v) => update("optOwners", v)}
            onPrice={(v) => update("optPriceUsd", v)}
          />
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <NumberField label="개발 비용 ($)" value={form.developmentUsd} onChange={(v) => update("developmentUsd", v)} />
          <NumberField label="마케팅 비용 ($)" value={form.marketingUsd} onChange={(v) => update("marketingUsd", v)} />
          <NumberField label="Monte Carlo 반복 (100~10,000)" value={form.iterations} min={100} max={10000} onChange={(v) => update("iterations", v)} />
        </div>
        <div className="flex items-center justify-between">
          <p className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
            슬라이더는 Iter 6+ — 현재는 직접 입력
          </p>
          <button
            type="submit"
            disabled={sim.isPending}
            className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            {sim.isPending ? "시뮬레이션 중…" : "시뮬레이션 실행"}
          </button>
        </div>
      </form>

      {sim.isPending ? <Loading label="Monte Carlo 시뮬레이션 진행 중…" /> : null}
      {sim.isError ? <ErrorBox error={sim.error} /> : null}
      {sim.data ? (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <ScenarioCard
              tone="rose"
              label="비관"
              data={sim.data.pessimistic}
            />
            <ScenarioCard
              tone="neutral"
              label="보통"
              data={sim.data.realistic}
            />
            <ScenarioCard
              tone="emerald"
              label="낙관"
              data={sim.data.optimistic}
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <Card variant="raised">
              <header className="flex items-center justify-between">
                <h2 className="font-semibold text-[var(--color-ink)]">
                  Monte Carlo {sim.data.monteCarlo.iterations}회
                </h2>
                {sim.data.monteCarlo.profitProbabilityPct !== null ? (
                  <Badge
                    tone={
                      sim.data.monteCarlo.profitProbabilityPct >= 80
                        ? "confidence-high"
                        : sim.data.monteCarlo.profitProbabilityPct >= 50
                          ? "confidence-med"
                          : "confidence-low"
                    }
                    dot
                    mono
                  >
                    흑자 {sim.data.monteCarlo.profitProbabilityPct.toFixed(1)}%
                  </Badge>
                ) : null}
              </header>
              <dl className="mt-4 grid grid-cols-3 gap-3">
                <Stat label="p10" value={`$${fmtCompact(Number(sim.data.monteCarlo.netRevenueP10))}`} size="sm" />
                <Stat label="p50" value={`$${fmtCompact(Number(sim.data.monteCarlo.netRevenueP50))}`} size="sm" tone="accent" />
                <Stat label="p90" value={`$${fmtCompact(Number(sim.data.monteCarlo.netRevenueP90))}`} size="sm" />
                <Stat label="평균" value={`$${fmtCompact(Number(sim.data.monteCarlo.netRevenueMean))}`} size="sm" />
                <Stat label="표준편차" value={`$${fmtCompact(Number(sim.data.monteCarlo.netRevenueStdDev))}`} size="sm" />
                <Stat
                  label="흑자 확률"
                  value={
                    sim.data.monteCarlo.profitProbabilityPct === null
                      ? "—"
                      : `${sim.data.monteCarlo.profitProbabilityPct.toFixed(1)}%`
                  }
                  size="sm"
                  tone="accent"
                />
              </dl>
              <p className="mt-3 text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
                seed {sim.data.assumptions.randomSeed} · refund{" "}
                {(sim.data.assumptions.refundRate * 100).toFixed(0)}% · steam cut{" "}
                {(sim.data.assumptions.steamCut * 100).toFixed(0)}%
              </p>
            </Card>

            <Card variant="raised">
              <h2 className="font-semibold text-[var(--color-ink)]">민감도 (OAT)</h2>
              <p className="mt-1 text-[var(--text-meta)] text-[var(--color-ink-muted)]">
                impactRatio 가 큰 변수일수록 결과에 민감 — 정밀 추정 우선
              </p>
              <ul className="mt-3 space-y-3">
                {sim.data.sensitivity.map((s) => (
                  <li key={s.variable} className="rounded-[var(--radius-input)] bg-[var(--color-surface-sunken)] p-3">
                    <div className="flex items-center justify-between">
                      <p className="font-medium text-[var(--color-ink)]">{s.variable}</p>
                      {s.impactRatio !== null ? (
                        <Badge tone="accent" mono>
                          {s.impactRatio.toFixed(2)}x
                        </Badge>
                      ) : null}
                    </div>
                    <p className="mt-1 text-[var(--text-meta)] text-[var(--color-ink-muted)] font-data">
                      비관 ${fmtCompact(Number(s.pessimisticNetUsd))} → 낙관 ${fmtCompact(Number(s.optimisticNetUsd))}
                    </p>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        </>
      ) : null}
    </div>
  );
}

type ScenarioGroupProps = {
  title: string;
  tone: "rose" | "emerald" | "neutral";
  owners: number;
  price: number;
  onOwners: (v: number) => void;
  onPrice: (v: number) => void;
};

function ScenarioGroup({ title, tone, owners, price, onOwners, onPrice }: ScenarioGroupProps) {
  const titleClass = {
    rose: "text-[var(--color-confidence-low)]",
    neutral: "text-[var(--color-ink)]",
    emerald: "text-[var(--color-confidence-high)]",
  }[tone];
  return (
    <fieldset className="rounded-[var(--radius-input)] border border-[var(--color-line)] p-3">
      <legend className={`px-1 text-xs font-semibold uppercase tracking-wide ${titleClass}`}>
        {title}
      </legend>
      <NumberField label="Owners" value={owners} onChange={onOwners} />
      <NumberField label="가격 (USD)" value={price} step={0.01} onChange={onPrice} />
    </fieldset>
  );
}

type NumberFieldProps = {
  label: string;
  value: number;
  onChange: (v: number) => void;
  step?: number;
  min?: number;
  max?: number;
};

function NumberField({ label, value, onChange, step, min, max }: NumberFieldProps) {
  return (
    <label className="mt-2 block text-xs">
      <span className="text-[var(--color-ink-muted)]">{label}</span>
      <input
        type="number"
        value={value}
        step={step}
        min={min}
        max={max}
        onChange={(e) => onChange(Number(e.target.value))}
        className="mt-1 w-full rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface)] px-2 py-1 text-sm font-data text-[var(--color-ink)]"
      />
    </label>
  );
}

type ScenarioCardProps = {
  tone: "rose" | "neutral" | "emerald";
  label: string;
  data: { developerNetUsd: string; profitUsd: string; roiPct: number | null; bepUnits: number | null };
};

function ScenarioCard({ tone, label, data }: ScenarioCardProps) {
  const accentTone = tone === "emerald" ? "up" : tone === "rose" ? "down" : "neutral";
  return (
    <Card variant="raised">
      <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
        {label}
      </p>
      <Stat
        label="개발사 net"
        value={`$${fmtCompact(Number(data.developerNetUsd))}`}
        size="lg"
        tone={accentTone}
      />
      <dl className="mt-3 space-y-1.5 text-[var(--text-meta)]">
        <div className="flex justify-between">
          <dt className="text-[var(--color-ink-muted)]">profit</dt>
          <dd className="font-data text-[var(--color-ink)]">${fmtCompact(Number(data.profitUsd))}</dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-[var(--color-ink-muted)]">ROI</dt>
          <dd className="font-data text-[var(--color-ink)]">
            {data.roiPct === null ? "—" : fmtPct(data.roiPct, 0)}
          </dd>
        </div>
        <div className="flex justify-between">
          <dt className="text-[var(--color-ink-muted)]">BEP units</dt>
          <dd className="font-data text-[var(--color-ink)]">{fmtInt(data.bepUnits)}</dd>
        </div>
      </dl>
    </Card>
  );
}
