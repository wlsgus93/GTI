import type { HTMLAttributes, ReactNode } from "react";

type CardProps = HTMLAttributes<HTMLDivElement> & {
  /** 시각 위계: 'sunken' < 'surface' < 'raised' < 'hero' */
  variant?: "surface" | "raised" | "sunken" | "hero";
  /** 페르소나 accent surface — surface-accent class 적용 */
  accent?: boolean;
  children: ReactNode;
};

export function Card({ variant = "surface", accent = false, className = "", children, ...rest }: CardProps) {
  const base = "rounded-[var(--radius-card)] border transition-colors";
  const surfaceClass =
    variant === "hero"
      ? "border-[var(--color-line)] p-8 lg:p-10"
      : variant === "raised"
        ? "border-[var(--color-line)] p-6 shadow-[0_4px_20px_-8px_rgba(0,0,0,0.08)]"
        : variant === "sunken"
          ? "border-transparent p-4"
          : "border-[var(--color-line)] p-5";

  const bgClass = accent
    ? "surface-accent"
    : variant === "sunken"
      ? "bg-[var(--color-surface-sunken)]"
      : variant === "hero"
        ? "bg-[var(--color-surface-raised)]"
        : "bg-[var(--color-surface-raised)]";

  return (
    <div className={`${base} ${surfaceClass} ${bgClass} ${className}`} {...rest}>
      {children}
    </div>
  );
}
