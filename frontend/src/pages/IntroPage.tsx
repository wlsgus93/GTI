import { motion, useReducedMotion } from "framer-motion";
import { useEffect, useState } from "react";
import { Link } from "react-router";
import { useAuth } from "@/auth/AuthContext";
import { EASE_OUT_EXPO } from "@/design/motion";

/**
 * IntroPage — 영화 오프닝 톤 cinematic 진입점.
 *
 * 흐름 (3 frame fade transition):
 *   Frame 1 (0~2.4s): "전 세계 게임 데이터를"
 *   Frame 2 (2.4~5.0s): "단 하나의 지능형 에이전트로"
 *   Frame 3 (5.0s+): 통계 + "시작하기" CTA
 *
 * - 사용자가 클릭하지 않으면 마지막 frame 에서 정지 (auto redirect X)
 * - "건너뛰기" 작은 link — 즉시 frame 3 점프
 * - localStorage `gti.seen-intro=true` 저장 → 다음 방문 시 router 가 skip
 * - 인증 사용자 → 자동 / 로 redirect (이미 본 사용자 가정)
 *
 * 룰 정합:
 * - `web/design-quality.md` cinematic / kinetic typography
 * - `15-agentic-ux.mdc` 검색창 X — 시작 CTA 만
 */

const TIMINGS = {
  frame1Show: 0,
  frame1Hide: 2.4,
  frame2Show: 2.4,
  frame2Hide: 5.0,
  frame3Show: 5.0,
};

const STATS = [
  { value: "9", label: "데이터 소스" },
  { value: "7", label: "분석 차원" },
  { value: "4", label: "페르소나 자동 추론" },
  { value: "3-Layer", label: "Hybrid LLM" },
];

export function IntroPage() {
  const { isAuthenticated } = useAuth();
  const reduced = useReducedMotion();
  // 재방문 (이미 본 사용자) 또는 모션 줄임 → frame 3 즉시
  const [skipped, setSkipped] = useState(() => {
    if (typeof window === "undefined") return false;
    return window.localStorage.getItem("gti.seen-intro") === "true";
  });

  // 인증된 사용자 → 즉시 / 로 (이미 가입자 — 인트로 보고 싶으면 명시 진입)
  useEffect(() => {
    if (isAuthenticated) {
      window.location.replace("/");
    }
  }, [isAuthenticated]);

  // intro 본 표시
  useEffect(() => {
    return () => {
      window.localStorage.setItem("gti.seen-intro", "true");
    };
  }, []);

  // 모션 줄임 사용자 → frame 3 즉시
  useEffect(() => {
    if (reduced) setSkipped(true);
  }, [reduced]);

  const t1Show = skipped ? 0 : TIMINGS.frame1Show;
  const t1Hide = skipped ? 0 : TIMINGS.frame1Hide;
  const t2Hide = skipped ? 0 : TIMINGS.frame2Hide;
  const t3Show = skipped ? 0 : TIMINGS.frame3Show;

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-[var(--color-surface)] px-6">
      {/* mesh background — intro 전용 강화 */}
      <div className="intro-mesh-bg" aria-hidden="true" />

      {/* skip 링크 — 우측 상단 */}
      {!skipped ? (
        <button
          type="button"
          onClick={() => setSkipped(true)}
          className="absolute right-6 top-6 z-10 text-xs text-[var(--color-ink-muted)] underline underline-offset-4 hover:text-[var(--color-ink)]"
        >
          건너뛰기 →
        </button>
      ) : null}

      {/* === Frame 1: "전 세계 게임 데이터를" === */}
      <motion.div
        initial={{ opacity: 0, y: 24, filter: "blur(10px)" }}
        animate={{
          opacity: skipped ? 0 : [0, 1, 1, 0],
          y: skipped ? 0 : [24, 0, 0, -16],
          filter: skipped ? "blur(0px)" : ["blur(10px)", "blur(0px)", "blur(0px)", "blur(6px)"],
        }}
        transition={{
          times: [0, 0.25, 0.85, 1],
          duration: t1Hide - t1Show + 0.5,
          delay: t1Show,
          ease: EASE_OUT_EXPO,
        }}
        className={`absolute ${skipped ? "hidden" : ""}`}
      >
        <h1 className="kinetic-text text-center text-[clamp(2.5rem,1.6rem+4vw,5rem)] font-bold leading-[1.05] tracking-tight">
          전 세계 게임 데이터를
        </h1>
      </motion.div>

      {/* === Frame 2: "단 하나의 지능형 에이전트로" === */}
      <motion.div
        initial={{ opacity: 0, y: 24, filter: "blur(10px)" }}
        animate={{
          opacity: skipped ? 0 : [0, 0, 1, 1, 0],
          y: skipped ? 0 : [24, 24, 0, 0, -16],
          filter: skipped
            ? "blur(0px)"
            : ["blur(10px)", "blur(10px)", "blur(0px)", "blur(0px)", "blur(6px)"],
        }}
        transition={{
          times: [0, 0.46, 0.55, 0.92, 1],
          duration: t2Hide,
          ease: EASE_OUT_EXPO,
        }}
        className={`absolute ${skipped ? "hidden" : ""}`}
      >
        <h1 className="kinetic-text text-center text-[clamp(2.5rem,1.6rem+4vw,5rem)] font-bold leading-[1.05] tracking-tight">
          단 하나의<br />
          <span className="text-[var(--color-accent-strong)]">지능형 에이전트</span>로
        </h1>
      </motion.div>

      {/* === Frame 3: 통계 + 시작하기 CTA === */}
      <motion.div
        initial={{ opacity: 0, y: 32 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: skipped ? 0.4 : 0.6,
          delay: skipped ? 0 : t3Show,
          ease: EASE_OUT_EXPO,
        }}
        className="z-10 max-w-3xl space-y-10 text-center"
      >
        <div className="space-y-4">
          <p className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.24em] text-[var(--color-accent-strong)]">
            GTI · GameTrend-Insight
          </p>
          <h1 className="kinetic-text text-[clamp(2rem,1.4rem+2.5vw,3.75rem)] font-bold leading-[1.05] tracking-tight">
            전 세계 게임 데이터를<br />
            <span className="text-[var(--color-accent-strong)]">단 하나의 지능형 에이전트</span>로
          </h1>
          <p className="mx-auto max-w-2xl text-[var(--color-ink-muted)]">
            Steam · Twitch · YouTube · Reddit · Apple · Google Play 등 9 소스를 동시 인입,
            <br />
            4 이해관계자 관점 자동 추론, 3-Layer Hybrid LLM 으로 비용 65% 절감.
          </p>
        </div>

        {/* 통계 4 */}
        <motion.div
          className="mx-auto grid w-full max-w-xl grid-cols-2 gap-3 sm:grid-cols-4"
          initial="hidden"
          animate="visible"
          variants={{
            visible: { transition: { staggerChildren: 0.08, delayChildren: skipped ? 0.1 : t3Show + 0.3 } },
          }}
        >
          {STATS.map((s) => (
            <motion.div
              key={s.label}
              variants={{
                hidden: { opacity: 0, y: 16 },
                visible: { opacity: 1, y: 0, transition: { duration: 0.4, ease: EASE_OUT_EXPO } },
              }}
              className="glass-card text-center !p-4"
            >
              <p className="font-data text-2xl font-bold text-[var(--color-accent-strong)]">
                {s.value}
              </p>
              <p className="mt-1 text-xs text-[var(--color-ink-muted)]">{s.label}</p>
            </motion.div>
          ))}
        </motion.div>

        {/* CTA */}
        <motion.div
          className="flex flex-col items-center gap-3"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.4,
            delay: skipped ? 0.3 : t3Show + 0.7,
            ease: EASE_OUT_EXPO,
          }}
        >
          <Link
            to="/signup"
            className="btn-micro group inline-flex items-center gap-2 rounded-[var(--radius-input)] bg-[var(--color-accent)] px-8 py-4 text-base font-semibold text-white shadow-2xl shadow-[var(--color-accent)]/30 transition hover:shadow-[var(--color-accent)]/50"
          >
            시작하기
            <span className="transition-transform duration-300 group-hover:translate-x-1">→</span>
          </Link>
          <Link
            to="/login"
            className="text-xs text-[var(--color-ink-muted)] underline underline-offset-4 hover:text-[var(--color-ink)]"
          >
            이미 계정이 있나요? 로그인
          </Link>
        </motion.div>

        {/* 하단 시그니처 */}
        <motion.p
          className="pt-4 text-[10px] uppercase tracking-[0.3em] text-[var(--color-ink-subtle)]"
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.6 }}
          transition={{ duration: 1, delay: skipped ? 0.5 : t3Show + 1.2 }}
        >
          BlackRabbit · WhiteRabbit Studio
        </motion.p>
      </motion.div>
    </div>
  );
}
