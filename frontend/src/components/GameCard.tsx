import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { useAddWatchlist } from "@/features/watchlist/hooks";
import type { TrendBoardItem } from "@/features/trend/api";
import { fmtPct } from "@/lib/format";

type GameCardProps = {
  game: TrendBoardItem;
};

export function GameCard({ game }: GameCardProps) {
  const { isAuthenticated } = useAuth();
  const addMutation = useAddWatchlist();
  const numericId = Number(game.id);
  const handleAdd = () => {
    if (!isAuthenticated) {
      window.alert("워치리스트는 로그인 후 사용 가능합니다.");
      return;
    }
    addMutation.mutate({ gameId: numericId });
  };

  const delta = game.ccuDeltaPct;

  return (
    <article className="rounded-xl border border-zinc-200 bg-white p-4 shadow-sm transition hover:border-zinc-300 dark:border-zinc-800 dark:bg-zinc-950 dark:hover:border-zinc-700">
      <div className="flex items-start justify-between gap-2">
        <Link
          to={`/games/${game.id}`}
          className="font-semibold text-zinc-900 hover:underline dark:text-zinc-50"
        >
          {game.title}
        </Link>
        <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-900 dark:bg-emerald-950 dark:text-emerald-200">
          TS {game.trendScore.toFixed(1)}
        </span>
      </div>
      <p className="mt-1 text-sm text-zinc-500 dark:text-zinc-400">
        {game.genre} · {game.platform}
      </p>
      <p className="mt-2 text-xs text-zinc-600 dark:text-zinc-500">
        CCU 변화율{" "}
        {delta === null ? (
          <span className="text-zinc-400">데이터 부족</span>
        ) : (
          <span className={delta >= 0 ? "text-emerald-600" : "text-rose-600"}>{fmtPct(delta)}</span>
        )}
      </p>
      <div className="mt-3 flex gap-2">
        <button
          type="button"
          className="rounded-lg border border-zinc-300 px-3 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-900"
          onClick={handleAdd}
          disabled={addMutation.isPending}
        >
          {addMutation.isPending ? "추가 중…" : "워치리스트"}
        </button>
        <Link
          to={`/workspace/compare?ids=${game.id}`}
          className="rounded-lg border border-zinc-300 px-3 py-1 text-xs font-medium text-zinc-700 hover:bg-zinc-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-900"
        >
          비교
        </Link>
      </div>
    </article>
  );
}
