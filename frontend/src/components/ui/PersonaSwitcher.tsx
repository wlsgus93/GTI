import { ALL_PERSONAS, PERSONA_THEMES } from "@/design/personaThemes";
import { usePersonaTheme } from "@/design/PersonaThemeContext";

type PersonaSwitcherProps = {
  /** 'compact' = 칩만 / 'detail' = 칩 + 호칭 + 톤 */
  variant?: "compact" | "detail";
};

/**
 * 4 페르소나 선택 — 활성 페르소나는 active accent, 비활성은 각자의 색을 dot 으로만 표시.
 *
 * 룰 정합: 4 페르소나 동등 (00-project-overview.mdc, 90-data-analyst-persona.mdc).
 * 어느 페르소나도 visual default가 아니며, 사용자가 명시 선택.
 */
export function PersonaSwitcher({ variant = "compact" }: PersonaSwitcherProps) {
  const { persona: active, setPersona } = usePersonaTheme();

  return (
    <fieldset
      role="radiogroup"
      aria-label="페르소나 선택"
      className={variant === "detail" ? "space-y-2" : "flex flex-wrap items-center gap-2"}
    >
      {variant === "compact" ? (
        <span className="text-[var(--text-meta)] font-semibold uppercase tracking-wide text-[var(--color-ink-muted)]">
          페르소나
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
