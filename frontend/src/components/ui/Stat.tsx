import type { ReactNode } from "react";

type StatProps = {
  label: string;
  value: ReactNode;
  /** 보조 정보 — 변화율, 단위, 기준 등 */
  hint?: ReactNode;
  /** 우측 상단 배지 (Badge 컴포넌트 등) */
  trailing?: ReactNode;
  /** value 사이즈 — 'sm' (sidebar) | 'md' (default card) | 'lg' (hero) */
  size?: "sm" | "md" | "lg";
  /** value 의 의미 톤 */
  tone?: "neutral" | "up" | "down" | "accent";
  /** mono 폰트 (수치) */
  mono?: boolean;
};

const SIZE_CLASS = {
  sm: "text-lg",
  md: "text-2xl",
  lg: "text-4xl lg:text-5xl",
};

const TONE_CLASS = {
  neutral: "text-[var(--color-ink)]",
  up: "text-[var(--color-confidence-high)]",
  down: "text-[var(--color-confidence-low)]",
  accent: "text-[var(--color-accent-strong)]",
};

export function Stat({ label, value, hint, trailing, size = "md", tone = "neutral", mono = true }: StatProps) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-start justify-between gap-2">
        <span className="text-[var(--text-meta)] font-medium uppercase tracking-wide text-[var(--color-ink-muted)]">
          {label}
        </span>
        {trailing}
      </div>
      <span className={`font-semibold leading-none ${SIZE_CLASS[size]} ${TONE_CLASS[tone]} ${mono ? "font-data" : ""}`}>
        {value}
      </span>
      {hint ? <span className="text-[var(--text-meta)] text-[var(--color-ink-subtle)]">{hint}</span> : null}
    </div>
  );
}
