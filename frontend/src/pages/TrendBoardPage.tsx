import { motion } from "framer-motion";
import { useMemo, useState } from "react";
import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { Stat } from "@/components/ui/Stat";
import { ConfidenceMeta } from "@/components/viz/ConfidenceMeta";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { staggerContainer, staggerItem } from "@/design/motion";
import { useTrends } from "@/features/trend/hooks";
import type { TrendBoardItem } from "@/features/trend/api";
import { useAddWatchlist } from "@/features/watchlist/hooks";
import { fmtCompact, fmtPct } from "@/lib/format";

type Sort = "trendScore" | "ccu" | "ccuAbs";

/**
 * 매거진/에디토리얼 레이아웃:
 *   - HERO: Top 1 게임 wide card (페르소나 accent gradient)
 *   - SECONDARY: Top 2~5 큰 카드 (2x2 grid)
 *   - LIST: Top 6+ 작은 행 (table-ish)
 *
 * 룰 정합:
 * - `web/design-quality.md` "grid-breaking editorial composition where appropriate"
 * - `web/design-quality.md` "intentional rhythm in spacing, not uniform padding everywhere"
 * - `90-data-analyst-persona.mdc` §1 — Confidence/소스 메타 hero 노출
 */
export function TrendBoardPage() {
  const { theme } = usePersonaTheme();
  const [sort, setSort] = useState<Sort>("trendScore");
  const [genre, setGenre] = useState<string>("all");
  const trends = useTrends(50);

  const sorted = useMemo(() => {
    if (!trends.data) return [];
    let rows = [...trends.data.content];
    if (genre !== "all") {
      rows = rows.filter((g) => g.genre.toLowerCase().includes(genre));
    }
    if (sort === "ccu") {
      rows.sort((a, b) => (b.ccuDeltaPct ?? -Infinity) - (a.ccuDeltaPct ?? -Infinity));
    } else if (sort === "ccuAbs") {
      rows.sort((a, b) => b.concurrentPlayers - a.concurrentPlayers);
    } else {
      rows.sort((a, b) => b.trendScore - a.trendScore);
    }
    return rows;
  }, [trends.data, genre, sort]);

  const hero = sorted[0];
  const secondary = sorted.slice(1, 5);
  const list = sorted.slice(5);

  const upCount = trends.data?.content.filter((g) => (g.ccuDeltaPct ?? 0) > 0).length ?? 0;
  const total = trends.data?.totalElements ?? 0;

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow={`게임 발굴 · ${theme.label} 시점`}
        title="오늘의 시장은 어디로 움직이고 있나"
        subtitle="9 소스 통합 TrendScore 기반 — 흥행도(D2) 차원이 1차 정렬 기준"
        trailing={
          trends.data ? (
            <div className="flex flex-col items-end gap-1.5">
              <Badge tone="accent" mono>
                상승 {upCount} / {total}
              </Badge>
              <span className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
                Top {Math.min(50, total)} 표시
              </span>
            </div>
          ) : null
        }
      />

      <ConfidenceMeta
        confidence={total > 0 ? "HIGH" : null}
        generatedAt={trends.dataUpdatedAt ? new Date(trends.dataUpdatedAt).toISOString() : undefined}
        sources={[
          { source: "Steam", grade: "Fact", capturedAt: trends.dataUpdatedAt ? new Date(trends.dataUpdatedAt).toISOString() : null },
        ]}
        method="log10 정규화 + 24h delta (W4+ Z-score 통일 예정)"
      />

      <FilterRow
        sort={sort}
        genre={genre}
        onSort={setSort}
        onGenre={setGenre}
        showCount={sorted.length}
        totalCount={total}
      />

      {trends.isLoading ? (
        <Loading label="9 소스 통합 점수 집계 중…" />
      ) : trends.isError ? (
        <ErrorBox error={trends.error} onRetry={() => trends.refetch()} />
      ) : sorted.length === 0 ? (
        <Empty
          label="조건에 맞는 게임이 없습니다"
          hint="필터를 완화하거나 ingestion 잡을 1회 실행하세요"
        />
      ) : (
        <>
          {hero ? <HeroGameCard game={hero} /> : null}
          {secondary.length > 0 ? (
            <motion.section
              aria-labelledby="secondary-heading"
              className="grid gap-4 sm:grid-cols-2"
              variants={staggerContainer}
              initial="hidden"
              animate="visible"
            >
              <h2 id="secondary-heading" className="sr-only">
                Top 2~5
              </h2>
              {secondary.map((g) => (
                <motion.div key={g.id} variants={staggerItem}>
                  <SecondaryGameCard game={g} />
                </motion.div>
              ))}
            </motion.section>
          ) : null}
          {list.length > 0 ? (
            <section aria-labelledby="list-heading">
              <h2
                id="list-heading"
                className="mb-3 text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]"
              >
                후순위 ({list.length}개)
              </h2>
              <Card variant="surface" className="!p-0">
                <ul role="list" className="divide-y divide-[var(--color-line)]">
                  {list.map((g, i) => (
                    <ListGameRow key={g.id} game={g} rank={i + 6} />
                  ))}
                </ul>
              </Card>
            </section>
          ) : null}
        </>
      )}
    </div>
  );
}

type FilterRowProps = {
  sort: Sort;
  genre: string;
  onSort: (s: Sort) => void;
  onGenre: (g: string) => void;
  showCount: number;
  totalCount: number;
};

function FilterRow({ sort, genre, onSort, onGenre, showCount, totalCount }: FilterRowProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <select
        value={sort}
        onChange={(e) => onSort(e.target.value as Sort)}
        aria-label="정렬"
        className="rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] px-3 py-1.5 text-sm text-[var(--color-ink)]"
      >
        <option value="trendScore">TrendScore 내림차순</option>
        <option value="ccu">CCU 변화율</option>
        <option value="ccuAbs">현재 CCU 절대값</option>
      </select>
      <select
        value={genre}
        onChange={(e) => onGenre(e.target.value)}
        aria-label="장르 필터"
        className="rounded-[var(--radius-input)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] px-3 py-1.5 text-sm text-[var(--color-ink)]"
      >
        <option value="all">전체 장르</option>
        <option value="fps">FPS</option>
        <option value="rpg">RPG</option>
        <option value="action">Action</option>
        <option value="simulation">Simulation</option>
      </select>
      <span className="ml-auto text-[var(--text-meta)] text-[var(--color-ink-subtle)]">
        {totalCount}개 중 {showCount}개 표시
      </span>
    </div>
  );
}

function HeroGameCard({ game }: { game: TrendBoardItem }) {
  const { isAuthenticated } = useAuth();
  const addMutation = useAddWatchlist();
  const handleAdd = () => {
    if (!isAuthenticated) {
      window.alert("워치리스트는 로그인 후 사용 가능합니다.");
      return;
    }
    addMutation.mutate({ gameId: Number(game.id) });
  };

  const delta = game.ccuDeltaPct;

  return (
    <Card variant="hero" accent>
      <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0 flex-1 space-y-3">
          <div className="flex items-center gap-2">
            <Badge tone="accent" mono>
              #1 TrendScore
            </Badge>
            <Badge tone="confidence-high" dot>
              Fact
            </Badge>
          </div>
          <Link
            to={`/games/${game.id}`}
            className="block text-3xl font-bold leading-tight tracking-tight text-[var(--color-ink)] hover:underline lg:text-4xl"
          >
            {game.title}
          </Link>
          <p className="text-[var(--color-ink-muted)]">
            {game.genre} · {game.platform} · appId {game.id}
          </p>
        </div>
        <div className="grid grid-cols-3 gap-6 lg:gap-10">
          <Stat label="TrendScore" value={game.trendScore.toFixed(1)} size="lg" tone="accent" />
          <Stat label="현재 CCU" value={fmtCompact(game.concurrentPlayers)} size="lg" />
          <Stat
            label="24h Δ"
            value={delta === null ? "—" : fmtPct(delta)}
            size="lg"
            tone={delta === null ? "neutral" : delta >= 0 ? "up" : "down"}
          />
        </div>
      </div>
      <div className="mt-6 flex flex-wrap gap-2">
        <Link
          to={`/games/${game.id}?tab=ai`}
          className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white transition hover:opacity-90"
        >
          AI 인사이트 받기 →
        </Link>
        <Link
          to={`/workspace/compare?ids=${game.id},1245620`}
          className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-4 py-2 text-sm font-medium text-[var(--color-ink)] transition hover:bg-[var(--color-surface-sunken)]"
        >
          ELDEN RING 과 비교
        </Link>
        <button
          type="button"
          onClick={handleAdd}
          disabled={addMutation.isPending}
          className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-4 py-2 text-sm font-medium text-[var(--color-ink)] transition hover:bg-[var(--color-surface-sunken)] disabled:opacity-50"
        >
          {addMutation.isPending ? "추가 중…" : "워치리스트 추가"}
        </button>
      </div>
    </Card>
  );
}

function SecondaryGameCard({ game }: { game: TrendBoardItem }) {
  const delta = game.ccuDeltaPct;
  return (
    <Card variant="raised">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <Link
            to={`/games/${game.id}`}
            className="block text-lg font-semibold text-[var(--color-ink)] hover:underline"
          >
            {game.title}
          </Link>
          <p className="text-[var(--text-meta)] text-[var(--color-ink-muted)]">
            {game.genre} · {game.platform}
          </p>
        </div>
        <Badge tone="accent" mono>
          TS {game.trendScore.toFixed(1)}
        </Badge>
      </div>
      <div className="mt-4 grid grid-cols-2 gap-4">
        <Stat label="CCU" value={fmtCompact(game.concurrentPlayers)} size="md" />
        <Stat
          label="24h Δ"
          value={delta === null ? "—" : fmtPct(delta)}
          size="md"
          tone={delta === null ? "neutral" : delta >= 0 ? "up" : "down"}
        />
      </div>
      <div className="mt-3 flex gap-2">
        <Link
          to={`/workspace/compare?ids=${game.id}`}
          className="text-[var(--text-meta)] font-medium text-[var(--color-accent-strong)] hover:underline"
        >
          비교 →
        </Link>
      </div>
    </Card>
  );
}

function ListGameRow({ game, rank }: { game: TrendBoardItem; rank: number }) {
  const delta = game.ccuDeltaPct;
  return (
    <li className="grid grid-cols-[3rem_1fr_auto_auto_auto] items-center gap-4 px-4 py-3">
      <span className="font-data text-sm text-[var(--color-ink-subtle)]">#{rank}</span>
      <Link
        to={`/games/${game.id}`}
        className="min-w-0 truncate font-medium text-[var(--color-ink)] hover:underline"
      >
        {game.title}
      </Link>
      <span className="text-[var(--text-meta)] text-[var(--color-ink-muted)]">{game.genre}</span>
      <span className="font-data text-sm text-[var(--color-ink)]">
        {fmtCompact(game.concurrentPlayers)}
      </span>
      <span
        className={`font-data text-sm ${
          delta === null
            ? "text-[var(--color-ink-subtle)]"
            : delta >= 0
              ? "text-[var(--color-confidence-high)]"
              : "text-[var(--color-confidence-low)]"
        }`}
      >
        {delta === null ? "—" : fmtPct(delta)}
      </span>
    </li>
  );
}
