import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { ErrorBox, Empty, Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Hero } from "@/components/ui/Hero";
import { useRemoveWatchlist, useWatchlist } from "@/features/watchlist/hooks";
import { fmtInt, fmtRelative } from "@/lib/format";

export function WatchlistPage() {
  const { isAuthenticated, auth } = useAuth();
  const list = useWatchlist();
  const remove = useRemoveWatchlist();

  return (
    <div className="space-y-[var(--space-section)]">
      <Hero
        eyebrow="P4 워치리스트"
        title="관심 게임 추적"
        subtitle="JWT 인증 후 추가 가능 — 트렌드 보드 / 게임 상세 카드의 '워치리스트' 버튼"
        trailing={
          isAuthenticated && list.data ? (
            <Badge tone="accent" mono>
              {list.data.length}개
            </Badge>
          ) : null
        }
      />

      {!isAuthenticated ? (
        <Card variant="raised" accent>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="font-semibold text-[var(--color-ink)]">로그인이 필요합니다</p>
              <p className="mt-1 text-sm text-[var(--color-ink-muted)]">
                워치리스트는 사용자 데이터입니다.
              </p>
            </div>
            <Link
              to="/login"
              className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-medium text-white"
            >
              로그인 →
            </Link>
          </div>
        </Card>
      ) : list.isLoading ? (
        <Loading label="워치리스트 불러오는 중…" />
      ) : list.isError ? (
        <ErrorBox error={list.error} showAuthHint onRetry={() => list.refetch()} />
      ) : !list.data || list.data.length === 0 ? (
        <Empty
          label={`${auth?.displayName ?? "사용자"}님 워치리스트가 비어있습니다`}
          hint="트렌드 보드 또는 게임 상세에서 '워치리스트' 버튼으로 추가하세요"
        />
      ) : (
        <Card variant="raised" className="!p-0">
          <ul className="divide-y divide-[var(--color-line)]">
            {list.data.map((item) => (
              <li key={item.id} className="flex items-start justify-between gap-4 p-4">
                <div className="min-w-0 flex-1">
                  <Link
                    to={`/games/${item.steamAppId ?? item.gameId}`}
                    className="font-semibold text-[var(--color-ink)] hover:underline"
                  >
                    {item.name}
                  </Link>
                  <p className="mt-0.5 text-[var(--text-meta)] text-[var(--color-ink-muted)] font-data">
                    CCU {fmtInt(item.latestCcu)} · 추가 {fmtRelative(item.addedAt)}
                  </p>
                  {item.note ? (
                    <p className="mt-1 text-xs italic text-[var(--color-ink-subtle)]">
                      메모: {item.note}
                    </p>
                  ) : null}
                </div>
                <button
                  type="button"
                  onClick={() => remove.mutate(item.gameId)}
                  disabled={remove.isPending}
                  className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-2.5 py-1 text-xs text-[var(--color-confidence-low)] transition hover:bg-[color-mix(in_oklch,var(--color-confidence-low)_8%,transparent)] disabled:opacity-50"
                >
                  {remove.isPending ? "제거 중…" : "제거"}
                </button>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
