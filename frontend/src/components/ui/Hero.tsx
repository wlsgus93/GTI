import type { ReactNode } from "react";
import { usePersonaTheme } from "@/design/PersonaThemeContext";

type HeroProps = {
  /** 작은 라벨 — eyebrow */
  eyebrow?: string;
  /** 큰 typography */
  title: ReactNode;
  /** 한 줄 설명 */
  subtitle?: ReactNode;
  /** 우측 영역 (CTA, 메타) */
  trailing?: ReactNode;
  /** 페르소나 accent gradient 적용 */
  accentBackground?: boolean;
};

/**
 * Hero — 페르소나 정체성을 큰 typography 로 표현.
 *
 * 룰 정합:
 * - `web/design-quality.md` — clear hierarchy through scale contrast (banned default 회피)
 * - `90-data-analyst-persona.mdc` — 페르소나 호칭/톤 시각화
 */
export function Hero({ eyebrow, title, subtitle, trailing, accentBackground = false }: HeroProps) {
  const { theme } = usePersonaTheme();
  const bgClass = accentBackground ? "surface-accent" : "bg-[var(--color-surface-raised)]";

  return (
    <section
      className={`overflow-hidden rounded-[var(--radius-card)] border border-[var(--color-line)] p-6 lg:p-10 ${bgClass}`}
      aria-label={`${theme.label} 컨텍스트 헤더`}
    >
      <div className="flex flex-wrap items-end justify-between gap-6">
        <div className="min-w-0 flex-1 space-y-2">
          {eyebrow ? (
            <p className="text-[var(--text-meta)] font-semibold uppercase tracking-[0.12em] text-accent">
              {eyebrow}
            </p>
          ) : null}
          <h1 className="text-[length:var(--text-headline)] font-bold leading-[1.05] tracking-tight kinetic-text">
            {title}
          </h1>
          {subtitle ? (
            <p className="max-w-[60ch] text-[length:var(--text-body)] leading-relaxed text-[var(--color-ink-muted)]">
              {subtitle}
            </p>
          ) : null}
        </div>
        {trailing ? <div className="shrink-0">{trailing}</div> : null}
      </div>
    </section>
  );
}
