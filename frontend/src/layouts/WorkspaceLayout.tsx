import { NavLink, Outlet } from "react-router";

const TABS = [
  { to: "/workspace/compare", label: "게임 비교", end: false },
  { to: "/workspace/watchlist", label: "워치리스트", end: false },
  { to: "/workspace/money-calc", label: "Money Calc", end: false },
] as const;

export function WorkspaceLayout() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-50">
          비교 · 워치리스트 · 매출 시뮬
        </h1>
        <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400">
          Compare / Tracker / Calc 통합 영역 (목업)
        </p>
      </div>
      <div className="flex flex-wrap gap-2 border-b border-zinc-200 dark:border-zinc-800">
        {TABS.map((t) => (
          <NavLink
            key={t.to}
            to={t.to}
            end={t.end}
            className={({ isActive }) =>
              [
                "rounded-t-lg px-4 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "border border-b-0 border-zinc-200 bg-white text-zinc-900 dark:border-zinc-700 dark:bg-zinc-950 dark:text-zinc-50"
                  : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100",
              ].join(" ")
            }
          >
            {t.label}
          </NavLink>
        ))}
      </div>
      <Outlet />
    </div>
  );
}
