import { motion } from "framer-motion";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { useAgentQuery } from "@/features/agent/hooks";
import type { AgentResponse } from "@/features/agent/api";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { EASE_OUT_EXPO, kineticChar, kineticContainer, staggerContainer, staggerItem } from "@/design/motion";
import { PERSONA_THEMES } from "@/design/personaThemes";
import type { Persona } from "@/features/insight/api";

/**
 * AgentHomePage — 풀 Agentic 진입점 (W9 옵션 B).
 *
 * 진정한 "검색창 X — 에이전트 시작" 구현:
 * - 큰 Hero (페르소나별 호칭 자동)
 * - 큰 CommandBar 중앙 (focus + glow-accent)
 * - 4 예시 chip (페르소나 시그널 자연 노출)
 * - 응답 inline (latest only — 누적은 AgentPanel)
 * - 데이터 풍부 페이지는 quick link 로 위임 (/discover, /compare, /watchlist 등)
 *
 * 룰 정합:
 * - `15-agentic-ux.mdc` "검색창 X — 에이전트 시작" 진정한 구현 ★
 * - `00-project-overview.mdc` 4 페르소나 동등 (자동 추론, 명시 X)
 * - `91-data-analyst-persona-global.mdc` Option E LLM 동적 분기
 */

const EXAMPLES: { label: string; query: string; emoji: string }[] = [
  { emoji: "🎮", label: "우리 인디 RPG 출시 시기", query: "우리 인디 RPG 출시 시기 추천해줘" },
  { emoji: "📈", label: "광고 채널 ROI 비교", query: "다음 분기 광고 캠페인 채널 우선순위 — ROI 기준" },
  { emoji: "🎯", label: "슈팅 게임 시장 동향", query: "Counter-Strike 2 같은 슈팅 게임 시장 동향" },
  { emoji: "💼", label: "신작 투자 리스크 평가", query: "신작 게임 투자 리스크 + 성공 확률" },
];

const QUICK_LINKS: { label: string; hint: string; to: string; icon: string }[] = [
  { icon: "📊", label: "트렌드 보드", hint: "TrendScore Top N", to: "/discover" },
  { icon: "🎮", label: "게임 상세", hint: "6 탭 (CCU·리뷰·커뮤니티·시청·D5·Compare)", to: "/games/1" },
  { icon: "🔀", label: "비교 분석", hint: "여러 게임 7 차원 비교", to: "/workspace/compare" },
  { icon: "📌", label: "워치리스트", hint: "관심 게임 모니터", to: "/workspace/watchlist" },
  { icon: "💸", label: "MoneyCalc", hint: "Monte Carlo 시뮬", to: "/workspace/moneycalc" },
  { icon: "🧪", label: "Pretotyping 검증", hint: "P7 자극물 4 케이스", to: "/verification" },
];

export function AgentHomePage() {
  const navigate = useNavigate();
  const { auth, isAuthenticated } = useAuth();
  const { theme } = usePersonaTheme();
  const inputRef = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState("");
  const [response, setResponse] = useState<AgentResponse | null>(null);
  const { mutate, isPending, error } = useAgentQuery();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // 미인증 시 가입 유도
  if (!isAuthenticated) {
    return <UnauthenticatedHero />;
  }

  const handleSubmit = (e?: FormEvent) => {
    e?.preventDefault();
    const query = draft.trim();
    if (!query || isPending) return;
    mutate({ query }, { onSuccess: setResponse });
  };

  const handleExampleClick = (query: string) => {
    setDraft(query);
    mutate({ query }, { onSuccess: setResponse });
  };

  const honorific = auth?.displayName ?? theme.honorific.replace(/님$/, "");

  return (
    <div className="space-y-10">
      {/* Hero */}
      <motion.header
        variants={kineticContainer}
        initial="hidden"
        animate="visible"
        className="space-y-3 pt-4 text-center"
      >
        <motion.p
          variants={kineticChar}
          className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.18em] text-[var(--color-accent-strong)]"
        >
          GTI · Game-Agent · {theme.label}
        </motion.p>
        <motion.h1
          variants={kineticChar}
          className="kinetic-text text-[clamp(2rem,1.4rem+2.5vw,3.5rem)] font-bold leading-[1.05] tracking-tight"
        >
          {response ? (
            <>분석 결과를 확인하시고,<br />다음 질문을 던져보세요</>
          ) : (
            <>{honorific}{theme.honorific.endsWith("님") ? "님" : ""},<br />오늘 어떤 도움이 필요하세요?</>
          )}
        </motion.h1>
        {!response ? (
          <motion.p variants={kineticChar} className="text-[var(--color-ink-muted)]">
            자유롭게 질문하시면, 에이전트가 9 데이터 소스에서 답을 만들어 드립니다.
          </motion.p>
        ) : null}
      </motion.header>

      {/* 4 예시 chip — 응답 전에만 노출 */}
      {!response ? (
        <motion.div
          className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
          {EXAMPLES.map((ex) => (
            <motion.button
              key={ex.query}
              variants={staggerItem}
              type="button"
              onClick={() => handleExampleClick(ex.query)}
              disabled={isPending}
              className="glass-card text-left disabled:opacity-50"
            >
              <span className="text-2xl">{ex.emoji}</span>
              <span className="mt-2 block text-sm font-medium text-[var(--color-ink)]">
                {ex.label}
              </span>
            </motion.button>
          ))}
        </motion.div>
      ) : null}

      {/* 큰 CommandBar 중앙 */}
      <form
        onSubmit={handleSubmit}
        className="glow-accent rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)]/80 p-1 backdrop-blur-md transition-all"
      >
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder={
              response
                ? "꼬리질문하거나 새 분석 요청..."
                : "예: '우리 인디 RPG 시장 분석', '광고 ROI 어디 쓸까?'"
            }
            disabled={isPending}
            className="flex-1 rounded-l-[var(--radius-card)] bg-transparent px-4 py-4 text-base text-[var(--color-ink)] placeholder:text-[var(--color-ink-subtle)] focus:outline-none disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={isPending || !draft.trim()}
            className="btn-micro mr-1 rounded-[var(--radius-input)] bg-[var(--color-accent)] px-5 py-2.5 text-sm font-semibold text-white shadow-md disabled:opacity-50"
          >
            {isPending ? "분석 중…" : "전송"}
          </button>
        </div>
      </form>

      {/* 에러 */}
      {error ? (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-lg border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-400"
        >
          {error.message}
        </motion.div>
      ) : null}

      {/* 응답 inline (latest only — 이력은 AgentPanel) */}
      {response ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.42, ease: EASE_OUT_EXPO }}
          className="space-y-4"
        >
          <div className="glass-card whitespace-pre-wrap text-[var(--color-ink)]">
            {response.content}
          </div>

          {/* 메타 + 추론된 페르소나 */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            {response.activePersona ? (
              <span className="rounded-full border border-[var(--color-accent)]/30 bg-[var(--color-accent-soft)]/40 px-3 py-1 text-[var(--color-accent-strong)]">
                관점: <strong>{PERSONA_THEMES[response.activePersona as Persona].label}</strong>
                {response.personaInferred ? " (자동 추론)" : ""}
              </span>
            ) : null}
            {response.classifierBlocked ? (
              <span className="rounded bg-amber-500/20 px-2 py-1 text-amber-400">
                Layer 1 차단 · 0 tokens
              </span>
            ) : (
              <>
                {response.model ? (
                  <span className="rounded bg-[var(--color-surface-sunken)] px-2 py-1 text-[var(--color-ink-muted)]">
                    {response.model}
                  </span>
                ) : null}
                <span className="rounded bg-[var(--color-surface-sunken)] px-2 py-1 text-[var(--color-ink-muted)]">
                  {response.promptTokens + response.completionTokens} tok
                </span>
              </>
            )}
            <span className="rounded bg-[var(--color-surface-sunken)] px-2 py-1 text-[var(--color-ink-muted)]">
              {response.latencyMs}ms
            </span>
          </div>

          {/* 다음 액션 */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <button
              type="button"
              onClick={() => {
                setResponse(null);
                setDraft("");
                inputRef.current?.focus();
              }}
              className="btn-micro rounded-[var(--radius-input)] border border-[var(--color-line)] px-4 py-2 text-sm font-medium text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
            >
              ← 새 질문
            </button>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => navigate("/discover")}
                className="btn-micro rounded-[var(--radius-input)] bg-[var(--color-accent)] px-4 py-2 text-sm font-semibold text-white shadow-md"
              >
                트렌드 보드 →
              </button>
            </div>
          </div>
        </motion.div>
      ) : null}

      {/* Quick links — 데이터 풍부 페이지로 위임 */}
      {!response ? (
        <motion.section
          aria-labelledby="quick-links-heading"
          className="space-y-3 pt-6"
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
        >
          <h2
            id="quick-links-heading"
            className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.12em] text-[var(--color-ink-muted)]"
          >
            바로 가기 — 깊이 있는 데이터 분석
          </h2>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {QUICK_LINKS.map((q) => (
              <motion.div key={q.to} variants={staggerItem}>
                <Link
                  to={q.to}
                  className="glass-card flex items-start gap-3 transition hover:border-[var(--color-accent)]/50"
                >
                  <span className="text-2xl">{q.icon}</span>
                  <span className="flex-1">
                    <span className="block text-sm font-semibold text-[var(--color-ink)]">
                      {q.label}
                    </span>
                    <span className="block text-xs text-[var(--color-ink-muted)]">
                      {q.hint}
                    </span>
                  </span>
                  <span aria-hidden className="text-[var(--color-ink-subtle)]">→</span>
                </Link>
              </motion.div>
            ))}
          </div>
        </motion.section>
      ) : null}
    </div>
  );
}

/** 미인증 사용자용 hero — 가입 유도 */
function UnauthenticatedHero() {
  const { theme } = usePersonaTheme();
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center space-y-6 text-center">
      <motion.h1
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: EASE_OUT_EXPO }}
        className="kinetic-text text-[clamp(2rem,1.4rem+3vw,4rem)] font-bold leading-[1.05] tracking-tight"
      >
        게임 시장 분석<br />에이전트와 함께
      </motion.h1>
      <motion.p
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1, duration: 0.4, ease: EASE_OUT_EXPO }}
        className="max-w-md text-[var(--color-ink-muted)]"
      >
        9 소스 데이터 · 4 이해관계자 관점 · LLM 인사이트.
        <br />
        가입 후 자연어로 질문하면 시스템이 당신의 관점을 자동 파악합니다.
      </motion.p>
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2, duration: 0.4, ease: EASE_OUT_EXPO }}
        className="flex gap-3"
      >
        <Link
          to="/signup"
          className="btn-micro rounded-[var(--radius-input)] bg-[var(--color-accent)] px-6 py-3 text-sm font-semibold text-white shadow-lg"
        >
          시작하기 →
        </Link>
        <Link
          to="/login"
          className="btn-micro rounded-[var(--radius-input)] border border-[var(--color-line)] px-6 py-3 text-sm font-semibold text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
        >
          로그인
        </Link>
      </motion.div>
      <p className="text-xs text-[var(--color-ink-subtle)]">
        현재 기본 관점: <span className="text-[var(--color-accent-strong)]">{theme.label}</span>
      </p>
    </div>
  );
}
