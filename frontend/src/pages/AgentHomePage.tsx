import { useState } from "react";
import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { CommandBar } from "@/components/ui/CommandBar";
import { Hero } from "@/components/ui/Hero";
import { PersonaSwitcher } from "@/components/ui/PersonaSwitcher";
import { Stat } from "@/components/ui/Stat";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { useTrends } from "@/features/trend/hooks";
import { useMultiInsight } from "@/features/insight/hooks";
import type { Persona } from "@/features/insight/api";
import { fmtCompact, fmtPct, fmtRelative } from "@/lib/format";

/**
 * AgentHomePage — 에이전트 우선 진입점.
 *
 * 룰 정합:
 * - `15-agentic-ux.mdc` 의 "검색창 X — 에이전트 시작" 철학 → CommandBar 핵심
 * - `00-project-overview.mdc` 4 페르소나 동등 → Hero 호칭/톤이 페르소나 따라 변화
 * - `web/design-quality.md` editorial / bento → 비대칭 grid (top game wide / 비교·워치 narrow)
 * - `90-data-analyst-persona.mdc` Confidence/cached 메타 시각화
 */
export function AgentHomePage() {
  const { auth, isAuthenticated } = useAuth();
  const { theme, persona } = usePersonaTheme();
  const trends = useTrends(8);

  const top = trends.data?.content[0];
  const featuredId = top?.id;
  const upCount = trends.data?.content.filter((g) => (g.ccuDeltaPct ?? 0) > 0).length ?? 0;

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow={`Game-Agent · ${theme.label}`}
        title={
          isAuthenticated ? (
            <>
              {auth?.displayName ?? auth?.email} {theme.honorific.replace(/님$/, "님")},<br />
              오늘의 시장은 어떻게 움직이고 있을까요?
            </>
          ) : (
            <>
              {theme.honorific}, <span className="text-accent">에이전트</span>와 함께<br />
              오늘의 시장을 한 번에 보세요
            </>
          )
        }
        subtitle={theme.tone}
        accentBackground
        trailing={
          <div className="flex flex-col items-end gap-2 text-right">
            {trends.data ? (
              <Badge tone="accent" mono>
                상승 {upCount}/{trends.data.totalElements} 게임
              </Badge>
            ) : null}
            {!isAuthenticated ? (
              <Link
                to="/signup"
                className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white"
              >
                회원가입 →
              </Link>
            ) : null}
          </div>
        }
      />

      <CommandBar />

      <section className="grid gap-4 lg:grid-cols-3">
        {/* Featured top game — 큰 카드 (lg:col-span-2) */}
        <FeaturedGameCard
          id={featuredId}
          title={top?.title}
          ccu={top?.concurrentPlayers}
          delta={top?.ccuDeltaPct ?? null}
          score={top?.trendScore}
          loading={trends.isLoading}
          error={trends.error}
          onRetry={() => trends.refetch()}
        />

        {/* 페르소나 상세 카드 */}
        <Card variant="raised" className="lg:col-span-1">
          <div className="flex flex-col gap-4">
            <div>
              <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
                현재 페르소나
              </p>
              <p className="mt-1 text-xl font-semibold text-[var(--color-ink)]">{theme.label}</p>
              <p className="mt-1 text-xs text-[var(--color-ink-muted)]">{theme.visualHint}</p>
            </div>
            <PersonaSwitcher variant="detail" />
            <p className="text-xs text-[var(--color-ink-subtle)]">
              모든 페이지·LLM 응답이 페르소나에 맞춰 분기됩니다.
            </p>
          </div>
        </Card>
      </section>

      <MultiPersonaPreview gameId={featuredId} initialPersonas={["INDIE", persona].filter((v, i, arr) => arr.indexOf(v) === i) as Persona[]} />

      <section className="grid gap-4 sm:grid-cols-3">
        <QuickAction
          title="트렌드 보드"
          subtitle="9 소스 통합 TrendScore Top N"
          href="/discover"
        />
        <QuickAction
          title="게임 비교"
          subtitle="Virtual Threads 병렬 비교"
          href="/workspace/compare?ids=730,1245620"
        />
        <QuickAction
          title="MoneyCalc"
          subtitle="3 시나리오 + Monte Carlo"
          href="/workspace/money-calc"
        />
      </section>
    </div>
  );
}

type FeaturedProps = {
  id: string | undefined;
  title: string | undefined;
  ccu: number | undefined;
  delta: number | null;
  score: number | undefined;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
};

function FeaturedGameCard({ id, title, ccu, delta, score, loading, error, onRetry }: FeaturedProps) {
  if (loading) {
    return (
      <Card variant="raised" className="lg:col-span-2">
        <Loading label="오늘의 Top 게임 불러오는 중…" />
      </Card>
    );
  }
  if (error) {
    return (
      <Card variant="raised" className="lg:col-span-2">
        <ErrorBox error={error} onRetry={onRetry} />
      </Card>
    );
  }
  if (!id) {
    return (
      <Card variant="raised" className="lg:col-span-2">
        <Empty
          label="아직 수집된 게임이 없습니다"
          hint="ingestion 잡을 1회 실행하면 시드 게임 10개가 적재됩니다"
        />
      </Card>
    );
  }
  return (
    <Card variant="hero" accent className="lg:col-span-2">
      <div className="flex flex-col gap-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.12em] text-accent">
              오늘의 Top
            </p>
            <Link
              to={`/games/${id}`}
              className="mt-1 block text-3xl font-bold leading-tight text-[var(--color-ink)] hover:underline"
            >
              {title}
            </Link>
          </div>
          <Badge tone="accent" mono>
            TS {score?.toFixed(1)}
          </Badge>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <Stat label="현재 CCU" value={fmtCompact(ccu)} hint="Steam 실시간" />
          <Stat
            label="24h 변화"
            value={delta === null ? "—" : fmtPct(delta)}
            tone={delta === null ? "neutral" : delta >= 0 ? "up" : "down"}
            hint="EWMA 권장 (W4+)"
          />
          <Stat label="appId" value={id} mono />
        </div>
        <div className="flex flex-wrap gap-2">
          <Link
            to={`/games/${id}?tab=ai`}
            className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-3 py-1.5 text-xs font-medium text-white"
          >
            AI 인사이트 받기 →
          </Link>
          <Link
            to={`/workspace/compare?ids=${id}`}
            className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-3 py-1.5 text-xs font-medium text-[var(--color-ink)]"
          >
            비교에 추가
          </Link>
        </div>
      </div>
    </Card>
  );
}

function MultiPersonaPreview({ gameId, initialPersonas }: { gameId: string | undefined; initialPersonas: Persona[] }) {
  const [shouldRun, setShouldRun] = useState(false);
  const [personas] = useState<Persona[]>(initialPersonas);
  const insights = useMultiInsight(shouldRun ? gameId : undefined, personas);

  if (!gameId) {
    return null;
  }

  return (
    <Card variant="raised">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
            멀티 페르소나 인사이트
          </p>
          <p className="mt-1 text-lg font-semibold text-[var(--color-ink)]">
            한 번에 {personas.length}개 페르소나 관점으로 분석
          </p>
          <p className="mt-1 text-xs text-[var(--color-ink-muted)]">
            Virtual Threads 병렬 호출 — wallClock = max(per-persona latency)
          </p>
        </div>
        <button
          type="button"
          disabled={insights.isFetching}
          onClick={() => setShouldRun(true)}
          className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {insights.isFetching ? "병렬 호출 중…" : "분석 실행"}
        </button>
      </div>

      {shouldRun ? (
        insights.isLoading ? (
          <div className="mt-4">
            <Loading label="Claude 멀티 페르소나 호출 — 첫 호출 5~15초" />
          </div>
        ) : insights.isError ? (
          <div className="mt-4">
            <ErrorBox error={insights.error} onRetry={() => insights.refetch()}>
              <p className="mt-2 text-xs">
                503 시: backend `.env` 에 ANTHROPIC_API_KEY 설정 필요 (docs/api-keys-guide.md)
              </p>
            </ErrorBox>
          </div>
        ) : insights.data ? (
          <div className="mt-4 space-y-3">
            <div className="flex items-center gap-3 text-xs">
              <Badge tone="accent" mono>
                wallClock {insights.data.totalLatencyMs}ms
              </Badge>
              <span className="text-[var(--color-ink-muted)]">{fmtRelative(insights.data.respondedAt)}</span>
            </div>
            <div className="grid gap-3 lg:grid-cols-2">
              {insights.data.perspectives.map((p) => (
                <Card key={p.persona} variant="surface">
                  <div className="flex items-center gap-2">
                    <Badge tone="accent" mono>
                      {p.personaLabel}
                    </Badge>
                    <Badge tone={p.cached ? "cached" : "fresh"} dot>
                      {p.cached ? "cached" : "fresh"}
                    </Badge>
                    {p.stale ? (
                      <Badge tone="stale" dot>
                        stale
                      </Badge>
                    ) : null}
                  </div>
                  <p className="mt-2 line-clamp-6 whitespace-pre-line text-sm leading-relaxed text-[var(--color-ink)]">
                    {p.summary}
                  </p>
                </Card>
              ))}
            </div>
          </div>
        ) : null
      ) : (
        <p className="mt-4 text-xs text-[var(--color-ink-subtle)]">
          페르소나를 바꿔 가며 같은 게임에 대한 다른 관점을 비교할 수 있습니다.
        </p>
      )}
    </Card>
  );
}

function QuickAction({ title, subtitle, href }: { title: string; subtitle: string; href: string }) {
  return (
    <Link
      to={href}
      className="group block rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] p-4 transition hover:border-[var(--color-accent)]"
    >
      <p className="text-sm font-semibold text-[var(--color-ink)] transition group-hover:text-[var(--color-accent-strong)]">
        {title} →
      </p>
      <p className="mt-1 text-xs text-[var(--color-ink-muted)]">{subtitle}</p>
    </Link>
  );
}
