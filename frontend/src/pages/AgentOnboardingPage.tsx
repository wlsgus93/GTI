import { motion } from "framer-motion";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { useAgentQuery } from "@/features/agent/hooks";
import type { AgentResponse } from "@/features/agent/api";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { EASE_OUT_EXPO, kineticChar, kineticContainer, staggerContainer, staggerItem } from "@/design/motion";
import type { Persona } from "@/features/insight/api";
import { PERSONA_THEMES } from "@/design/personaThemes";

/**
 * W9 옵션 C — Agentic Onboarding.
 *
 * 진정한 Agentic UX 의 첫 진입점:
 * - 가입 직후 (or 첫 로그인 + chat_session 없음) 진입
 * - 페르소나 카드 강제 선택 X
 * - 큰 인사 + 4 예시 chip (각 페르소나 시그널 자연 노출)
 * - 사용자 답변 → 백엔드가 자동 페르소나 추론 (LocalIntentClassifier)
 * - 추론 완료 → 메인 (/) 로 이동, mesh 색상 자동 morph
 *
 * 룰 정합:
 * - `15-agentic-ux.mdc` "검색창 X — 에이전트 시작" — 진정한 구현
 * - `00-project-overview.mdc` 4 페르소나 동등 — 시스템이 자동 추론
 * - `91-data-analyst-persona-global.mdc` Option E (LLM 동적 분기) → 우선순위 1번 격상
 */

const EXAMPLES: { label: string; query: string; signal: Persona }[] = [
  {
    label: "🎮 우리 인디 RPG 출시 시기 잡고 싶어",
    query: "우리 인디 RPG 출시 시기 추천해줘",
    signal: "INDIE",
  },
  {
    label: "📈 다음 분기 광고 예산 어디 쓸지 고민",
    query: "다음 분기 광고 캠페인 채널 우선순위 추천 — ROI 기준",
    signal: "MARKETER",
  },
  {
    label: "🎯 Counter-Strike 2 같은 슈팅 시장 어때?",
    query: "Counter-Strike 2 같은 슈팅 게임 시장 동향 분석",
    signal: "PUBLISHER",
  },
  {
    label: "💼 신작 투자 검토 중 — 리스크 평가",
    query: "신작 게임 투자 리스크 + 성공 확률 평가",
    signal: "INVESTOR",
  },
];

export function AgentOnboardingPage() {
  const navigate = useNavigate();
  const { setPersona, theme } = usePersonaTheme();
  const inputRef = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState("");
  const [response, setResponse] = useState<AgentResponse | null>(null);
  const { mutate, isPending, error } = useAgentQuery();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleSubmit = (e?: FormEvent) => {
    e?.preventDefault();
    const query = draft.trim();
    if (!query || isPending) return;
    mutate(
      { query },
      {
        onSuccess: (res) => {
          setResponse(res);
          // W9 옵션 C — 백엔드가 추론한 persona 로 즉시 morph
          if (res.activePersona) setPersona(res.activePersona);
        },
      },
    );
  };

  const handleExampleClick = (example: (typeof EXAMPLES)[number]) => {
    setDraft(example.query);
    // 즉시 호출 (UX — 1 클릭이면 응답)
    mutate(
      { query: example.query },
      {
        onSuccess: (res) => {
          setResponse(res);
          if (res.activePersona) setPersona(res.activePersona);
        },
      },
    );
  };

  const goToMain = () => navigate("/", { replace: true });

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center px-4 py-10">
      {/* mesh 배경 — MainLayout 밖이라 별도 div */}
      <div className="mesh-bg" aria-hidden="true" />

      <motion.div
        className="w-full max-w-2xl space-y-8"
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: EASE_OUT_EXPO }}
      >
        {/* Hero */}
        <motion.header
          variants={kineticContainer}
          initial="hidden"
          animate="visible"
          className="space-y-3 text-center"
        >
          <motion.p
            variants={kineticChar}
            className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.18em] text-[var(--color-accent-strong)]"
          >
            GTI · Game-Agent
          </motion.p>
          <motion.h1
            variants={kineticChar}
            className="kinetic-text text-[clamp(1.875rem,1.4rem+2vw,3rem)] font-bold leading-[1.05] tracking-tight"
          >
            안녕하세요 👋
            <br />
            오늘 어떤 도움이 필요하세요?
          </motion.h1>
          <motion.p
            variants={kineticChar}
            className="text-[var(--color-ink-muted)]"
          >
            자유롭게 질문하시면, 에이전트가 당신의 관점을 자동으로 이해합니다.
          </motion.p>
        </motion.header>

        {/* 4 예시 chip — 각 페르소나 시그널 자연 노출 */}
        {!response ? (
          <motion.div
            className="grid gap-3 sm:grid-cols-2"
            variants={staggerContainer}
            initial="hidden"
            animate="visible"
          >
            {EXAMPLES.map((ex) => (
              <motion.button
                key={ex.label}
                variants={staggerItem}
                type="button"
                onClick={() => handleExampleClick(ex)}
                disabled={isPending}
                className="glass-card text-left disabled:opacity-50"
              >
                <span className="block text-sm font-medium text-[var(--color-ink)]">
                  {ex.label}
                </span>
              </motion.button>
            ))}
          </motion.div>
        ) : null}

        {/* 큰 CommandBar */}
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
              placeholder="예: '우리 인디 게임 시장 분석', '광고 예산 추천'..."
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

        {/* 응답 */}
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

            {/* 추론된 페르소나 표시 */}
            {response.activePersona ? (
              <div className="flex items-center justify-between rounded-lg border border-[var(--color-accent)]/30 bg-[var(--color-accent-soft)]/40 px-4 py-3 text-sm">
                <div className="flex items-center gap-2">
                  <span className="text-[var(--color-ink-muted)]">에이전트가 추론한 관점:</span>
                  <span className="font-bold text-[var(--color-accent-strong)]">
                    {PERSONA_THEMES[response.activePersona].label}
                  </span>
                  {response.personaInferred ? (
                    <span className="rounded bg-[var(--color-accent)]/20 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-[var(--color-accent-strong)]">
                      자동 추론
                    </span>
                  ) : null}
                </div>
                <span className="text-xs text-[var(--color-ink-muted)]">
                  변경은 헤더 뱃지에서
                </span>
              </div>
            ) : null}

            {/* CTA */}
            <div className="flex items-center justify-between gap-3 pt-2">
              <button
                type="button"
                onClick={() => {
                  setResponse(null);
                  setDraft("");
                  inputRef.current?.focus();
                }}
                className="btn-micro rounded-[var(--radius-input)] border border-[var(--color-line)] px-4 py-2 text-sm font-medium text-[var(--color-ink-muted)] hover:text-[var(--color-ink)]"
              >
                다른 질문하기
              </button>
              <button
                type="button"
                onClick={goToMain}
                className="btn-micro rounded-[var(--radius-input)] bg-[var(--color-accent)] px-5 py-2.5 text-sm font-semibold text-white shadow-md"
              >
                트렌드 보드로 →
              </button>
            </div>
          </motion.div>
        ) : null}

        {/* 페르소나 인식 안내 (응답 전) */}
        {!response ? (
          <p className="text-center text-xs text-[var(--color-ink-subtle)]">
            현재 기본 관점:{" "}
            <span className="font-semibold text-[var(--color-accent-strong)]">
              {theme.label}
            </span>
            {" — 첫 질문 후 시스템이 자동 조정합니다"}
          </p>
        ) : null}
      </motion.div>
    </div>
  );
}
