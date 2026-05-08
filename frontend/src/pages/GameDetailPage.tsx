import { useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useAuth } from "@/auth/AuthContext";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";
import { ConfidenceMeta } from "@/components/viz/ConfidenceMeta";
import { DimensionRadar, type DimensionAxis } from "@/components/viz/DimensionRadar";
import { useCommunityDimension } from "@/features/dimension/hooks";
import { useEconomics } from "@/features/economics/hooks";
import { useCcuSeries, useGameDetail, usePlayerInsight } from "@/features/game/hooks";
import { useInsight } from "@/features/insight/hooks";
import { PERSONA_LABEL, type Persona } from "@/features/insight/api";
import { useAddWatchlist } from "@/features/watchlist/hooks";
import { fmtCompact, fmtInt, fmtPct, fmtRelative } from "@/lib/format";

const GAME_TABS = [
  { param: "overview", label: "개요" },
  { param: "ai", label: "AI 인사이트" },
  { param: "ccu", label: "동접 (CCU)" },
  { param: "players", label: "플레이어" },
  { param: "revenue", label: "매출" },
  { param: "cpv", label: "단가" },
  { param: "dimensions", label: "7차원" },
] as const;

type TabParam = (typeof GAME_TABS)[number]["param"];
type TabLabel = (typeof GAME_TABS)[number]["label"];

const PARAM_BY_LABEL = Object.fromEntries(GAME_TABS.map((t) => [t.label, t.param])) as Record<
  TabLabel,
  TabParam
>;

const RANGE_OPTIONS = ["24h", "7d", "30d", "90d"] as const;
type RangeOption = (typeof RANGE_OPTIONS)[number];

const PERSONA_OPTIONS: Persona[] = ["INDIE", "PUBLISHER", "MARKETER", "INVESTOR"];

export function GameDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isAuthenticated } = useAuth();
  const addMutation = useAddWatchlist();

  const tabParam = (searchParams.get("tab") ?? "overview") as TabParam;
  const tab: TabLabel = useMemo(() => {
    const row = GAME_TABS.find((t) => t.param === tabParam);
    return row?.label ?? "개요";
  }, [tabParam]);

  const selectTab = (label: TabLabel) => {
    setSearchParams({ tab: PARAM_BY_LABEL[label] }, { replace: true });
  };

  const detail = useGameDetail(id);
  const numericId = detail.data?.id;

  const handleAddWatch = () => {
    if (!isAuthenticated) {
      window.alert("워치리스트는 로그인 후 사용 가능합니다.");
      return;
    }
    if (!numericId) {
      return;
    }
    addMutation.mutate({ gameId: numericId });
  };

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow={detail.data?.developer ? detail.data.developer : "게임 상세"}
        title={detail.data?.name ?? (detail.isLoading ? "불러오는 중…" : "게임")}
        subtitle={
          <span>
            appId <span className="font-data text-[var(--color-ink)]">{detail.data?.steamAppId ?? id}</span>
            {detail.data?.genres?.length ? ` · ${detail.data.genres.join(", ")}` : ""}
            {detail.data?.releaseDate ? ` · 출시 ${detail.data.releaseDate}` : ""}
          </span>
        }
        trailing={
          <div className="flex flex-col items-end gap-2">
            {detail.data?.latestCcu ? (
              <Stat
                label="현재 CCU"
                value={fmtCompact(detail.data.latestCcu)}
                hint={detail.data.ccuDeltaPct === null ? "Δ —" : `Δ ${fmtPct(detail.data.ccuDeltaPct)}`}
                tone={
                  detail.data.ccuDeltaPct === null
                    ? "neutral"
                    : detail.data.ccuDeltaPct >= 0
                      ? "up"
                      : "down"
                }
                size="md"
              />
            ) : null}
            <div className="flex gap-2">
              <Link
                to={`/workspace/compare?ids=${id}`}
                className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-3 py-1.5 text-xs font-medium text-[var(--color-ink)]"
              >
                비교
              </Link>
              <button
                type="button"
                onClick={handleAddWatch}
                disabled={addMutation.isPending || !numericId}
                className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50"
              >
                {addMutation.isPending ? "추가 중…" : "워치리스트"}
              </button>
            </div>
          </div>
        }
      />

      {detail.data ? (
        <ConfidenceMeta
          confidence="HIGH"
          generatedAt={detail.data.updatedAt}
          sources={[{ source: "Steam", grade: "Fact", capturedAt: detail.data.updatedAt }]}
        />
      ) : null}

      {detail.isLoading ? (
        <Loading label="게임 정보 불러오는 중…" />
      ) : detail.isError ? (
        <ErrorBox error={detail.error} onRetry={() => detail.refetch()} />
      ) : null}

      <nav
        role="tablist"
        aria-label="게임 상세 탭"
        className="flex flex-wrap gap-1 border-b border-[var(--color-line)]"
      >
        {GAME_TABS.map(({ param, label }) => (
          <button
            key={param}
            type="button"
            role="tab"
            aria-selected={tab === label}
            onClick={() => selectTab(label)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-medium transition ${
              tab === label
                ? "border-[var(--color-accent)] text-[var(--color-accent-strong)]"
                : "border-transparent text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
            }`}
          >
            {label}
          </button>
        ))}
      </nav>

      {tab === "개요" ? <OverviewTab detail={detail.data} /> : null}
      {tab === "AI 인사이트" ? <InsightTab gameId={numericId} /> : null}
      {tab === "동접 (CCU)" ? <CcuTab gameId={numericId} /> : null}
      {tab === "플레이어" ? <PlayersTab gameId={numericId} /> : null}
      {tab === "매출" ? <RevenueTab gameId={numericId} /> : null}
      {tab === "단가" ? <CpvTab gameId={numericId} /> : null}
      {tab === "7차원" ? <DimensionsTab gameId={numericId} /> : null}
    </div>
  );
}

// ============================================================
// Tab: 개요
// ============================================================

function OverviewTab({ detail }: { detail: ReturnType<typeof useGameDetail>["data"] }) {
  if (!detail) return null;
  return (
    <Card variant="raised">
      <div className="grid gap-6 sm:grid-cols-3">
        <Stat label="현재 CCU" value={fmtInt(detail.latestCcu)} />
        <Stat
          label="24h Δ"
          value={detail.ccuDeltaPct === null ? "—" : fmtPct(detail.ccuDeltaPct)}
          tone={detail.ccuDeltaPct === null ? "neutral" : detail.ccuDeltaPct >= 0 ? "up" : "down"}
        />
        <Stat label="출시일" value={detail.releaseDate ?? "—"} mono={false} />
      </div>
      {detail.description ? (
        <p className="mt-6 whitespace-pre-line text-sm leading-relaxed text-[var(--color-ink-muted)]">
          {detail.description}
        </p>
      ) : (
        <p className="mt-6 text-sm text-[var(--color-ink-subtle)]">
          상세 설명이 아직 동기화되지 않았습니다.
        </p>
      )}
    </Card>
  );
}

// ============================================================
// Tab: AI 인사이트
// ============================================================

function InsightTab({ gameId }: { gameId: number | undefined }) {
  const [persona, setPersona] = useState<Persona>("INDIE");
  const insight = useInsight(gameId, persona);

  return (
    <Card variant="raised">
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
          페르소나
        </span>
        {PERSONA_OPTIONS.map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => setPersona(p)}
            className={`inline-flex items-center gap-1 rounded-[var(--radius-pill)] border px-3 py-1 text-xs font-medium transition ${
              persona === p
                ? "border-transparent bg-[var(--color-accent)] text-white"
                : "border-[var(--color-line)] text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
            }`}
          >
            {PERSONA_LABEL[p]}
          </button>
        ))}
      </div>

      {insight.isLoading ? (
        <div className="mt-4">
          <Loading label="LLM 호출 중… (cache miss는 5~15초)" />
        </div>
      ) : insight.isError ? (
        <div className="mt-4">
          <ErrorBox error={insight.error} onRetry={() => insight.refetch()}>
            <p className="mt-2 text-xs">503 시: ANTHROPIC_API_KEY 미설정 — `docs/api-keys-guide.md`</p>
          </ErrorBox>
        </div>
      ) : insight.data ? (
        <article className="mt-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone={insight.data.cached ? "cached" : "fresh"} dot>
              {insight.data.cached ? "cached" : "fresh"}
            </Badge>
            {insight.data.stale ? (
              <Badge tone="stale" dot>
                stale fallback
              </Badge>
            ) : null}
            <Badge tone="neutral" mono>
              {insight.data.model}
            </Badge>
            <Badge tone="neutral" mono>
              tokens {insight.data.totalTokens}
            </Badge>
            <span className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
              {fmtRelative(insight.data.generatedAt)}
            </span>
          </div>
          <p className="whitespace-pre-line text-[length:var(--text-body)] leading-relaxed text-[var(--color-ink)]">
            {insight.data.summary}
          </p>
        </article>
      ) : null}
    </Card>
  );
}

// ============================================================
// Tab: CCU 시계열
// ============================================================

function CcuTab({ gameId }: { gameId: number | undefined }) {
  const [range, setRange] = useState<RangeOption>("30d");
  const series = useCcuSeries(gameId, range);

  if (series.isLoading) return <Loading label="CCU 시계열 불러오는 중…" />;
  if (series.isError) return <ErrorBox error={series.error} onRetry={() => series.refetch()} />;
  const points = series.data?.points ?? [];

  const chartData = points.map((p) => ({
    label: new Date(p.capturedAt).toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" }),
    ccu: p.concurrentPlayers,
  }));

  return (
    <Card variant="raised">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold text-[var(--color-ink)]">동접 (CCU)</h2>
        <div className="flex gap-1">
          {RANGE_OPTIONS.map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRange(r)}
              className={`rounded-[var(--radius-input)] px-3 py-1 text-xs font-medium font-data transition ${
                range === r
                  ? "bg-[var(--color-accent)] text-white"
                  : "border border-[var(--color-line)] text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
              }`}
            >
              {r}
            </button>
          ))}
        </div>
      </header>
      {chartData.length === 0 ? (
        <Empty label="이 기간에 수집된 스냅샷이 없습니다" hint="ingestion 잡 실행 후 다시 시도" />
      ) : (
        <div className="mt-4 h-[300px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-line)" />
              <XAxis dataKey="label" tick={{ fontSize: 12, fill: "var(--color-ink-muted)" }} />
              <YAxis
                tick={{ fontSize: 12, fill: "var(--color-ink-muted)" }}
                tickFormatter={(v) => fmtCompact(v as number)}
              />
              <Tooltip
                contentStyle={{
                  background: "var(--color-surface-raised)",
                  border: "1px solid var(--color-line)",
                  borderRadius: "var(--radius-input)",
                  color: "var(--color-ink)",
                  fontSize: 12,
                }}
                formatter={(v) => fmtInt(typeof v === "number" ? v : Number(v))}
              />
              <Line
                type="monotone"
                dataKey="ccu"
                stroke="var(--color-accent)"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </Card>
  );
}

// ============================================================
// Tab: 플레이어
// ============================================================

function PlayersTab({ gameId }: { gameId: number | undefined }) {
  const players = usePlayerInsight(gameId);
  if (players.isLoading) return <Loading label="플레이어 분석 데이터 불러오는 중…" />;
  if (players.isError) return <ErrorBox error={players.error} onRetry={() => players.refetch()} />;
  if (!players.data) return null;
  const { players: stats, twitchViewers, mentions, lastUpdated } = players.data;

  return (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card variant="raised">
          <Stat label="현재 CCU" value={fmtInt(stats.concurrentPlayers)} />
        </Card>
        <Card variant="raised">
          <Stat label="Twitch 시청자" value={fmtInt(twitchViewers)} />
        </Card>
        <Card variant="raised">
          <Stat
            label="긍정 리뷰"
            value={
              stats.reviewScorePercent === null
                ? "—"
                : `${stats.reviewScorePercent.toFixed(1)}%`
            }
            hint={`${fmtInt(stats.reviewScoreTotal)}개 중`}
          />
        </Card>
        <Card variant="raised">
          <Stat label="갱신" value={fmtRelative(lastUpdated)} mono={false} size="sm" />
        </Card>
      </div>

      <Card variant="raised">
        <h3 className="font-semibold text-[var(--color-ink)]">커뮤니티 멘션</h3>
        {mentions.length === 0 ? (
          <div className="mt-3">
            <Empty label="아직 수집된 멘션이 없습니다" hint="YouTube/Reddit ingestion 잡 확인" />
          </div>
        ) : (
          <table className="mt-3 w-full text-left text-sm">
            <thead>
              <tr className="border-b border-[var(--color-line)] text-[var(--color-ink-muted)]">
                <th className="py-2 font-medium">소스</th>
                <th className="py-2 font-medium">멘션 수</th>
                <th className="py-2 font-medium">측정 시각</th>
              </tr>
            </thead>
            <tbody>
              {mentions.map((m) => (
                <tr key={m.source} className="border-b border-[var(--color-line)] last:border-0">
                  <td className="py-2 font-medium">{m.source}</td>
                  <td className="py-2 font-data">{fmtInt(m.mentionCount)}</td>
                  <td className="py-2 text-xs text-[var(--color-ink-subtle)]">
                    {fmtRelative(m.capturedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </div>
  );
}

// ============================================================
// Tab: 매출
// ============================================================

function RevenueTab({ gameId }: { gameId: number | undefined }) {
  const eco = useEconomics(gameId);
  if (eco.isLoading) return <Loading label="매출 추정 불러오는 중…" />;
  if (eco.isError) return <ErrorBox error={eco.error} onRetry={() => eco.refetch()} />;
  if (!eco.data?.revenue) {
    return <Empty label="매출 추정에 필요한 데이터 부족" hint="SteamSpy owners + Steam 가격 필요" />;
  }
  const r = eco.data.revenue;
  return (
    <div className="space-y-4">
      <ConfidenceMeta
        confidence={eco.data.confidence}
        generatedAt={eco.data.lastUpdated ?? undefined}
        sources={[
          { source: "SteamSpy", grade: "Range", capturedAt: eco.data.lastUpdated },
          { source: "Steam Store", grade: "Fact", capturedAt: eco.data.lastUpdated },
        ]}
        method="ownersMid × price × 0.95(refund) × 0.70(cut)"
      />
      <div className="grid gap-4 sm:grid-cols-3">
        <Card variant="raised">
          <Stat
            label="Owners (mid)"
            value={fmtCompact(r.ownersMid)}
            hint={`범위 ${fmtCompact(r.ownersLow)} – ${fmtCompact(r.ownersHigh)}`}
          />
        </Card>
        <Card variant="raised">
          <Stat label="가격 (USD)" value={r.priceUsd ? `$${r.priceUsd}` : "—"} />
        </Card>
        <Card variant="raised">
          <Stat
            label="개발사 net 추정"
            value={r.developerNet ? `$${fmtCompact(Number(r.developerNet))}` : "—"}
            tone="accent"
          />
        </Card>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Card variant="surface">
          <Stat
            label="총 매출 (gross)"
            value={r.grossLifetimeRevenue ? `$${fmtCompact(Number(r.grossLifetimeRevenue))}` : "—"}
          />
        </Card>
        <Card variant="surface">
          <Stat
            label="환불 차감 후"
            value={r.afterRefundRevenue ? `$${fmtCompact(Number(r.afterRefundRevenue))}` : "—"}
          />
        </Card>
        <Card variant="surface">
          <Stat label="추정 DAU" value={fmtCompact(r.estimatedDau)} />
        </Card>
        <Card variant="surface">
          <Stat label="추정 MAU" value={fmtCompact(r.estimatedMau)} />
        </Card>
      </div>
      <Link
        to={`/workspace/money-calc?gameId=${gameId ?? ""}`}
        className="inline-block text-sm font-medium text-[var(--color-accent-strong)] underline"
      >
        Money Calc 시나리오 시뮬레이션 →
      </Link>
    </div>
  );
}

// ============================================================
// Tab: 단가 (CPV)
// ============================================================

function CpvTab({ gameId }: { gameId: number | undefined }) {
  const eco = useEconomics(gameId);
  if (eco.isLoading) return <Loading label="단가 데이터 불러오는 중…" />;
  if (eco.isError) return <ErrorBox error={eco.error} onRetry={() => eco.refetch()} />;
  const u = eco.data?.unitEconomics;
  if (!u) return <Empty label="단가 분석에 필요한 데이터 부족" />;

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Card variant="raised">
        <Stat
          label="View → Play 비율"
          value={u.viewToPlayRatio === null ? "—" : u.viewToPlayRatio.toFixed(2)}
          hint="CCU / Twitch 시청자 — 1.0+ 이면 시청→플레이 전환 강함"
        />
      </Card>
      <Card variant="raised">
        <Stat
          label="Mention → Play 비율"
          value={u.mentionToPlayRatio === null ? "—" : u.mentionToPlayRatio.toFixed(2)}
          hint="멘션 / CCU — 입소문 강도"
        />
      </Card>
      <Card variant="raised">
        <Stat
          label="가격 효율"
          value={u.priceEfficiency === null ? "—" : u.priceEfficiency.toFixed(2)}
          hint="CCU per $1"
        />
      </Card>
      <Card variant="raised">
        <Stat
          label="긍정 리뷰당 가격"
          value={u.reviewCostPerPositive ? `$${u.reviewCostPerPositive}` : "—"}
        />
      </Card>
    </div>
  );
}

// ============================================================
// Tab: 7차원
// ============================================================

function DimensionsTab({ gameId }: { gameId: number | undefined }) {
  const community = useCommunityDimension(gameId);

  // 7차원 — 현재 D5 (커뮤니티) + D2 (트렌드: 게임 상세 latestCcu 활용 가능)
  // 미구현은 placeholder 회색
  const detail = useGameDetail(gameId);

  const axes: DimensionAxis[] = useMemo(() => {
    const d5Score = (() => {
      if (!community.data?.activityZScore && community.data?.activityZScore !== 0) return null;
      // Z-score (-2 ~ +2) → 0~100 매핑
      const z = community.data.activityZScore;
      return Math.max(0, Math.min(100, Math.round(((z + 2) / 4) * 100)));
    })();
    const d2Score = (() => {
      if (!detail.data?.latestCcu) return null;
      // log10 정규화 (1~1M → 0~100)
      const log = Math.log10(Math.max(1, detail.data.latestCcu));
      return Math.max(0, Math.min(100, Math.round((log / 6) * 100)));
    })();
    return [
      { key: "D1", label: "출시 동향", score: null, implemented: false },
      { key: "D2", label: "흥행 (CCU)", score: d2Score, implemented: true },
      { key: "D3", label: "그래픽 성향", score: null, implemented: false },
      { key: "D4", label: "장르 클러스터", score: null, implemented: false },
      { key: "D5", label: "커뮤니티", score: d5Score, implemented: true },
      { key: "D6", label: "흥행 요인", score: null, implemented: false },
      { key: "D7", label: "지역·플랫폼", score: null, implemented: false },
    ];
  }, [community.data, detail.data]);

  if (community.isLoading) return <Loading label="7차원 분석 불러오는 중…" />;

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
      <Card variant="raised">
        <header className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-lg font-semibold text-[var(--color-ink)]">7차원 분석</h2>
            <p className="mt-1 text-[var(--text-meta)] text-[var(--color-ink-muted)]">
              현재 구현 2/7 — 회색 영역은 향후 차원 (`docs/analysis-dimensions.md`)
            </p>
          </div>
          {community.data?.confidence ? (
            <Badge
              tone={
                community.data.confidence === "HIGH"
                  ? "confidence-high"
                  : community.data.confidence === "MEDIUM"
                    ? "confidence-med"
                    : "confidence-low"
              }
              dot
            >
              D5 {community.data.confidence}
            </Badge>
          ) : null}
        </header>
        <div className="mt-4">
          <DimensionRadar axes={axes} height={360} />
        </div>
      </Card>

      <Card variant="raised">
        <h3 className="font-semibold text-[var(--color-ink)]">D5 커뮤니티 활성도</h3>
        {community.isError ? (
          <div className="mt-3">
            <ErrorBox error={community.error} onRetry={() => community.refetch()} />
          </div>
        ) : community.data ? (
          <div className="mt-3 space-y-3">
            <Stat
              label="활성도 분류"
              value={community.data.activityClass}
              hint={
                community.data.activityZScore === null
                  ? "Z-score 미계산"
                  : `Z-score ${community.data.activityZScore.toFixed(2)} (전체 평균 대비)`
              }
              tone="accent"
              mono={false}
            />
            <div>
              <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
                총 멘션
              </p>
              <p className="mt-1 font-data text-2xl text-[var(--color-ink)]">
                {fmtInt(community.data.totalMentions)}
              </p>
            </div>
            {community.data.painPoints.length > 0 ? (
              <div>
                <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
                  Pain Point (LLM Sentiment)
                </p>
                <ul className="mt-1 space-y-1 text-sm">
                  {community.data.painPoints.slice(0, 3).map((p) => (
                    <li key={p.topic} className="text-[var(--color-ink)]">
                      <span className="font-medium">{p.topic}</span> ·{" "}
                      <span className="text-[var(--color-ink-muted)]">{p.description}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ) : (
              <p className="text-xs text-[var(--color-ink-subtle)]">
                Pain Point LLM Sentiment 통합은 W7+ 후속 (mention 텍스트 컬럼 + Claude Sentiment 호출)
              </p>
            )}
          </div>
        ) : null}
      </Card>
    </div>
  );
}
