import { AnimatePresence, motion } from "framer-motion";
import { Suspense, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { AgentPanel } from "@/components/AgentPanel";
import { AppFooter } from "@/components/AppFooter";
import { Loading } from "@/components/AsyncState";
import { Badge } from "@/components/ui/Badge";
import { PersonaSwitcher } from "@/components/ui/PersonaSwitcher";
import { SAMPLE_GAME_ID } from "@/constants/routes";
import { pageTransition, pageTransitionConfig } from "@/design/motion";

function sidebarItemActive(to: string, pathname: string, search: string): boolean {
  if (to === "/") {
    return pathname === "/";
  }
  if (to.startsWith("/workspace")) {
    return pathname.startsWith("/workspace");
  }
  if (to === "/discover") {
    return pathname === "/discover";
  }
  if (to === "/cpv") {
    return pathname === "/cpv";
  }
  const [path, query] = to.split("?");
  if (pathname !== path) {
    return false;
  }
  const have = new URLSearchParams(search);
  if (!query) {
    const tab = have.get("tab");
    return !tab || tab === "overview";
  }
  const want = new URLSearchParams(query);
  return have.get("tab") === want.get("tab");
}

const PRIMARY_NAV = [
  { to: "/", label: "Game-Agent", hint: "1분 요약 + CommandBar" },
  { to: `/games/${SAMPLE_GAME_ID}`, label: "게임 상세", hint: "P2 6 탭" },
  { to: `/games/${SAMPLE_GAME_ID}?tab=players`, label: "플레이어 분석", hint: "리뷰 + Twitch" },
  { to: "/discover", label: "게임 발굴", hint: "TrendScore Top N" },
  { to: "/workspace/compare", label: "비교 · 워치 · Calc", hint: "P3 · P4 · P5" },
  { to: "/cpv", label: "스트리머 단가", hint: "CPV 협업" },
] as const;

const DISCOVER_EXTRA = [{ to: "/publishers", label: "퍼블리셔 (P8)" }] as const;

const INTERNAL_NAV = [
  { to: "/internal", label: "자사 리포트 (P6)" },
  { to: "/verification", label: "검증 모듈 (P7)" },
  { to: "/campaigns", label: "캠페인 (P9)" },
  { to: "/reports", label: "트렌드 리포트 (P10)" },
] as const;

export function MainLayout() {
  const [agentOpen, setAgentOpen] = useState(false);
  const { pathname, search } = useLocation();
  const { auth, isAuthenticated, logout } = useAuth();

  return (
    <div className="relative flex min-h-screen flex-col bg-[var(--color-surface)]">
      {/* Iter 7 — Persona-Adaptive Mesh Gradient (페르소나에 따라 색상 자동 변환) */}
      <div className="mesh-bg" aria-hidden="true" />

      <header className="sticky top-0 z-40 border-b border-[var(--color-line)] bg-[var(--color-surface-overlay)] backdrop-blur">
        <div className="flex flex-wrap items-center gap-3 px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2">
            <span className="text-lg font-bold tracking-tight text-[var(--color-ink)]">GTI</span>
            <Badge tone="accent" dot>
              Game-Agent
            </Badge>
          </NavLink>
          <div className="ml-auto flex flex-wrap items-center gap-3">
            {/* W9 옵션 C — 작은 뱃지 (자동 추론 메타 표시 + 클릭 시 명시 변경) */}
            <div className="hidden md:block">
              <PersonaSwitcher variant="badge" />
            </div>
            <button
              type="button"
              className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-3 py-1.5 text-sm font-medium text-[var(--color-ink)] transition hover:bg-[var(--color-surface-sunken)]"
              onClick={() => setAgentOpen(true)}
            >
              패널
            </button>
            {isAuthenticated ? (
              <>
                <span className="hidden text-xs text-[var(--color-ink-muted)] md:inline">
                  {auth?.displayName ?? auth?.email}
                </span>
                <button
                  type="button"
                  onClick={logout}
                  className="rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-3 py-1.5 text-sm font-medium text-[var(--color-ink)] transition hover:bg-[var(--color-surface-sunken)]"
                >
                  로그아웃
                </button>
              </>
            ) : (
              <>
                <NavLink
                  to="/login"
                  className="rounded-[var(--radius-input)] px-3 py-1.5 text-sm font-medium text-[var(--color-ink-muted)] transition hover:text-[var(--color-ink)]"
                >
                  로그인
                </NavLink>
                <NavLink
                  to="/signup"
                  className="rounded-[var(--radius-input)] bg-[var(--color-accent)] px-3 py-1.5 text-sm font-medium text-white transition hover:opacity-90"
                >
                  회원가입
                </NavLink>
              </>
            )}
          </div>
        </div>
      </header>

      <div className="flex flex-1">
        <aside className="hidden w-60 shrink-0 border-r border-[var(--color-line)] bg-[var(--color-surface)] lg:block">
          <nav className="sticky top-[57px] space-y-1 overflow-y-auto p-3" aria-label="주요 메뉴">
            <p className="px-2 pb-2 text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-subtle)]">
              탐색
            </p>
            {PRIMARY_NAV.map((item) => {
              const active = sidebarItemActive(item.to, pathname, search);
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={`flex flex-col rounded-[var(--radius-input)] px-2 py-2 text-sm transition-colors ${
                    active
                      ? "bg-[var(--color-accent-soft)] text-[var(--color-accent-strong)]"
                      : "text-[var(--color-ink-muted)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-ink)]"
                  }`}
                >
                  <span className="font-medium">{item.label}</span>
                  <span className="mt-0.5 text-xs opacity-70">{item.hint}</span>
                </Link>
              );
            })}
            <div className="border-t border-[var(--color-line)] pt-3">
              <p className="px-2 pb-2 text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-subtle)]">
                발굴 보조
              </p>
              {DISCOVER_EXTRA.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `block rounded-[var(--radius-input)] px-2 py-2 text-sm transition ${
                      isActive
                        ? "bg-[var(--color-accent-soft)] text-[var(--color-accent-strong)]"
                        : "text-[var(--color-ink-muted)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-ink)]"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
            <div className="border-t border-[var(--color-line)] pt-3">
              <p className="px-2 pb-2 text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[oklch(58%_0.16_35)]">
                Internal
              </p>
              {INTERNAL_NAV.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `block rounded-[var(--radius-input)] px-2 py-2 text-sm transition ${
                      isActive
                        ? "bg-[color-mix(in_oklch,var(--color-marketer)_18%,transparent)] text-[var(--color-marketer-strong)]"
                        : "text-[var(--color-ink-muted)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-ink)]"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          </nav>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <main className="mx-auto w-full max-w-[1240px] flex-1 px-4 py-6 lg:px-8">
            <Suspense fallback={<Loading label="페이지 로드 중…" />}>
              <AnimatePresence mode="wait">
                <motion.div
                  key={pathname}
                  variants={pageTransition}
                  initial="initial"
                  animate="animate"
                  exit="exit"
                  transition={pageTransitionConfig}
                >
                  <Outlet />
                </motion.div>
              </AnimatePresence>
            </Suspense>
          </main>
          <AppFooter />
        </div>
      </div>

      {/* 모바일: 하단 간단 네비 */}
      <nav
        className="sticky bottom-0 z-30 flex border-t border-[var(--color-line)] bg-[var(--color-surface-raised)] lg:hidden"
        aria-label="모바일 메뉴"
      >
        {(
          [
            { to: "/", label: "Agent", active: pathname === "/" },
            { to: "/discover", label: "발굴", active: pathname === "/discover" },
            { to: `/games/${SAMPLE_GAME_ID}`, label: "게임", active: pathname.startsWith("/games/") },
            { to: "/workspace/compare", label: "도구", active: pathname.startsWith("/workspace") },
          ] as const
        ).map((item) => (
          <Link
            key={item.label}
            to={item.to}
            className={`flex-1 py-3 text-center text-xs font-medium transition ${
              item.active
                ? "text-[var(--color-accent-strong)]"
                : "text-[var(--color-ink-muted)]"
            }`}
          >
            {item.label}
          </Link>
        ))}
      </nav>

      {agentOpen ? (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-black/40"
          aria-label="패널 닫기"
          onClick={() => setAgentOpen(false)}
        />
      ) : null}
      <AgentPanel open={agentOpen} onClose={() => setAgentOpen(false)} />
    </div>
  );
}
