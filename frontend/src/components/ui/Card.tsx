import type { HTMLAttributes, ReactNode } from "react";

type CardProps = HTMLAttributes<HTMLDivElement> & {
  /** 시각 위계: 'sunken' < 'surface' < 'raised' < 'hero' < 'glass' (Iter 7 — Cybernetic Bento) */
  variant?: "surface" | "raised" | "sunken" | "hero" | "glass";
  /** 페르소나 accent surface — surface-accent class 적용 */
  accent?: boolean;
  /** Glassmorphism 강제 적용 (variant 와 무관) */
  glass?: boolean;
  /** hover lift + glow effect (Iter 7 — micro interaction) */
  interactive?: boolean;
  children: ReactNode;
};

export function Card({
  variant = "surface",
  accent = false,
  glass = false,
  interactive = false,
  className = "",
  children,
  ...rest
}: CardProps) {
  const base = "rounded-[var(--radius-card)]";
  const padClass =
    variant === "hero"
      ? "p-8 lg:p-10"
      : variant === "raised"
        ? "p-6"
        : variant === "sunken"
          ? "p-4"
          : variant === "glass"
            ? "p-6"
            : "p-5";

  const useGlass = glass || variant === "glass";
  const bgClass = useGlass
    ? "glass-card"
    : accent
      ? "surface-accent border border-[var(--color-line)] transition-colors"
      : variant === "sunken"
        ? "bg-[var(--color-surface-sunken)] border border-transparent transition-colors"
        : "bg-[var(--color-surface-raised)] border border-[var(--color-line)] shadow-[0_4px_20px_-8px_rgba(0,0,0,0.08)] transition-all duration-[280ms] ease-[cubic-bezier(0.16,1,0.3,1)]";

  const interactClass = interactive
    ? useGlass
      ? "" // glass-card 가 hover 자체 처리
      : "hover:-translate-y-1 hover:border-[color-mix(in_oklch,var(--color-accent)_50%,transparent)] hover:shadow-[0_18px_36px_-12px_color-mix(in_oklch,var(--color-accent)_25%,transparent)] cursor-pointer"
    : "";

  return (
    <div className={`${base} ${padClass} ${bgClass} ${interactClass} ${className}`} {...rest}>
      {children}
    </div>
  );
}
