import type { ReactNode } from "react";

type BadgeTone =
  | "neutral"
  | "accent"
  | "confidence-high"
  | "confidence-med"
  | "confidence-low"
  | "fresh"
  | "stale"
  | "cached"
  | "warning";

type BadgeProps = {
  tone?: BadgeTone;
  /** 작은 dot 표시 (semantic 강화) */
  dot?: boolean;
  /** mono 폰트 (수치 강조) */
  mono?: boolean;
  children: ReactNode;
};

const TONE_CLASS: Record<BadgeTone, string> = {
  neutral: "bg-[var(--color-surface-sunken)] text-[var(--color-ink-muted)]",
  accent: "bg-[var(--color-accent-soft)] text-[var(--color-accent-strong)]",
  "confidence-high": "bg-[color-mix(in_oklch,var(--color-confidence-high)_20%,transparent)] text-[var(--color-confidence-high)]",
  "confidence-med": "bg-[color-mix(in_oklch,var(--color-confidence-med)_22%,transparent)] text-[oklch(45%_0.13_80)]",
  "confidence-low": "bg-[color-mix(in_oklch,var(--color-confidence-low)_18%,transparent)] text-[var(--color-confidence-low)]",
  fresh: "bg-[color-mix(in_oklch,var(--color-fresh)_16%,transparent)] text-[var(--color-fresh)]",
  stale: "bg-[color-mix(in_oklch,var(--color-stale)_22%,transparent)] text-[oklch(45%_0.13_80)]",
  cached: "bg-[color-mix(in_oklch,var(--color-cached)_16%,transparent)] text-[var(--color-cached)]",
  warning: "bg-[color-mix(in_oklch,var(--color-marketer)_20%,transparent)] text-[var(--color-marketer-strong)]",
};

const DOT_CLASS: Record<BadgeTone, string> = {
  neutral: "bg-[var(--color-ink-subtle)]",
  accent: "bg-[var(--color-accent)]",
  "confidence-high": "bg-[var(--color-confidence-high)]",
  "confidence-med": "bg-[var(--color-confidence-med)]",
  "confidence-low": "bg-[var(--color-confidence-low)]",
  fresh: "bg-[var(--color-fresh)]",
  stale: "bg-[var(--color-stale)]",
  cached: "bg-[var(--color-cached)]",
  warning: "bg-[var(--color-marketer)]",
};

export function Badge({ tone = "neutral", dot = false, mono = false, children }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-[var(--radius-pill)] px-2.5 py-0.5 text-[0.6875rem] font-medium ${
        mono ? "font-data" : ""
      } ${TONE_CLASS[tone]}`}
    >
      {dot ? <span className={`h-1.5 w-1.5 rounded-full ${DOT_CLASS[tone]}`} aria-hidden /> : null}
      {children}
    </span>
  );
}
