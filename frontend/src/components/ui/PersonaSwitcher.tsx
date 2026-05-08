import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { ALL_PERSONAS, PERSONA_THEMES } from "@/design/personaThemes";
import { usePersonaTheme } from "@/design/PersonaThemeContext";
import { EASE_OUT_EXPO } from "@/design/motion";

type PersonaSwitcherProps = {
  /**
   * 'badge' = 작은 dropdown 뱃지 (헤더용 — W9 옵션 C default) ★
   * 'compact' = 칩만 (legacy)
   * 'detail' = 큰 카드 (legacy — 회원가입 X 후 사용 X)
   */
  variant?: "badge" | "compact" | "detail";
  /** "에이전트가 추론" 표시 (자동 추론된 페르소나면 true). UI 가 자동 추론 메타 노출. */
  inferred?: boolean;
};

/**
 * 4 페르소나 선택 — W9 옵션 C 부터 default = 'badge' (작은 dropdown).
 *
 * 룰 정합:
 * - 4 페르소나 동등 (00-project-overview.mdc, 90-data-analyst-persona.mdc)
 * - W9 옵션 C — 사용자 명시 X, 시스템 자동 추론 default. 명시 변경은 헤더 뱃지로만.
 */
export function PersonaSwitcher({ variant = "badge", inferred = false }: PersonaSwitcherProps) {
  const { persona: active, setPersona } = usePersonaTheme();

  if (variant === "badge") {
    return <PersonaBadge active={active} setPersona={setPersona} inferred={inferred} />;
  }

  // legacy variant — compact / detail (점진 제거 예정)
  return (
    <fieldset
      role="radiogroup"
      aria-label="페르소나 선택"
      className={variant === "detail" ? "space-y-2" : "flex flex-wrap items-center gap-2"}
    >
      {variant === "compact" ? (
        <span className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
          관점
        </span>
      ) : null}
      {ALL_PERSONAS.map((p) => {
        const t = PERSONA_THEMES[p];
        const isActive = p === active;
        if (variant === "detail") {
          return (
            <button
              key={p}
              type="button"
              role="radio"
              aria-checked={isActive}
              onClick={() => setPersona(p)}
              className={`group flex w-full items-center justify-between rounded-[var(--radius-card)] border p-3 text-left transition ${
                isActive
                  ? "border-[var(--color-accent)] bg-[var(--color-accent-soft)]"
                  : "border-[var(--color-line)] bg-[var(--color-surface-raised)] hover:border-[var(--color-line-strong)]"
              }`}
            >
              <span className="flex min-w-0 items-center gap-3">
                <span
                  aria-hidden
                  className="h-3 w-3 shrink-0 rounded-full"
                  style={{ background: `var(--color-${t.cssVar})` }}
                />
                <span className="min-w-0">
                  <p className="text-sm font-semibold text-[var(--color-ink)]">{t.label}</p>
                  <p className="truncate text-xs text-[var(--color-ink-muted)]">{t.tone}</p>
                </span>
              </span>
              {isActive ? (
                <span className="shrink-0 text-xs font-medium text-[var(--color-accent-strong)]">활성</span>
              ) : null}
            </button>
          );
        }
        // compact
        return (
          <button
            key={p}
            type="button"
            role="radio"
            aria-checked={isActive}
            onClick={() => setPersona(p)}
            className={`inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] border px-3 py-1 text-xs font-medium transition ${
              isActive
                ? "border-transparent bg-[var(--color-accent)] text-white"
                : "border-[var(--color-line)] text-[var(--color-ink-muted)] hover:border-[var(--color-line-strong)] hover:text-[var(--color-ink)]"
            }`}
          >
            {!isActive ? (
              <span
                aria-hidden
                className="h-2 w-2 rounded-full"
                style={{ background: `var(--color-${t.cssVar})` }}
              />
            ) : null}
            {t.label}
          </button>
        );
      })}
    </fieldset>
  );
}

/**
 * W9 옵션 C — 헤더용 작은 dropdown 뱃지.
 *
 * 닫혔을 때: "관점: <persona> ▾" 작은 chip
 * 열렸을 때: 4 페르소나 list + 자동 추론 표기
 */
function PersonaBadge({
  active,
  setPersona,
  inferred,
}: {
  active: typeof ALL_PERSONAS[number];
  setPersona: (p: typeof ALL_PERSONAS[number]) => void;
  inferred: boolean;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const t = PERSONA_THEMES[active];

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    if (open) document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={`현재 관점: ${t.label}, 클릭 시 변경`}
        className="btn-micro inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--color-line)] bg-[var(--color-surface-raised)]/60 px-3 py-1.5 text-xs font-medium text-[var(--color-ink)] backdrop-blur transition hover:border-[var(--color-accent)]"
      >
        <span
          aria-hidden
          className="h-2 w-2 rounded-full"
          style={{ background: `var(--color-${t.cssVar})` }}
        />
        <span className="text-[var(--color-ink-muted)]">관점</span>
        <span className="font-semibold">{t.label}</span>
        {inferred ? (
          <span className="rounded bg-[var(--color-accent)]/15 px-1 py-0.5 text-[9px] font-bold uppercase tracking-wide text-[var(--color-accent-strong)]">
            자동
          </span>
        ) : null}
        <span aria-hidden className="text-[var(--color-ink-muted)]">▾</span>
      </button>

      <AnimatePresence>
        {open ? (
          <motion.ul
            role="listbox"
            initial={{ opacity: 0, y: -6, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -6, scale: 0.96 }}
            transition={{ duration: 0.18, ease: EASE_OUT_EXPO }}
            className="glass-card absolute right-0 top-full z-50 mt-2 w-64 !p-2"
          >
            <li className="px-3 py-2 text-[10px] font-semibold uppercase tracking-wide text-[var(--color-ink-subtle)]">
              관점 명시 변경 (자동 추론 override)
            </li>
            {ALL_PERSONAS.map((p) => {
              const pt = PERSONA_THEMES[p];
              const isActive = p === active;
              return (
                <li key={p}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={isActive}
                    onClick={() => {
                      setPersona(p);
                      setOpen(false);
                    }}
                    className={`flex w-full items-center justify-between rounded-[var(--radius-input)] px-3 py-2 text-sm transition ${
                      isActive
                        ? "bg-[var(--color-accent-soft)] text-[var(--color-accent-strong)]"
                        : "text-[var(--color-ink-muted)] hover:bg-[var(--color-surface-sunken)] hover:text-[var(--color-ink)]"
                    }`}
                  >
                    <span className="flex items-center gap-2">
                      <span
                        aria-hidden
                        className="h-2.5 w-2.5 rounded-full"
                        style={{ background: `var(--color-${pt.cssVar})` }}
                      />
                      <span className="font-medium">{pt.label}</span>
                    </span>
                    {isActive ? <span className="text-xs">●</span> : null}
                  </button>
                </li>
              );
            })}
          </motion.ul>
        ) : null}
      </AnimatePresence>
    </div>
  );
}
