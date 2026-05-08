import type { ReactNode } from "react";
import { ApiError } from "@/lib/api/error";

type LoadingProps = { label?: string };

export function Loading({ label = "불러오는 중…" }: LoadingProps) {
  return (
    <div
      className="flex items-center gap-2.5 rounded-[var(--radius-card)] border border-[var(--color-line)] bg-[var(--color-surface-raised)] px-4 py-3 text-[length:var(--text-meta)] text-[var(--color-ink-muted)]"
      role="status"
      aria-live="polite"
    >
      <span
        className="inline-block h-1.5 w-1.5 animate-pulse rounded-full bg-[var(--color-accent)]"
        aria-hidden
      />
      {label}
    </div>
  );
}

type ErrorProps = {
  error: unknown;
  onRetry?: () => void;
  /** 인증 만료 시 사용자 안내 표시 여부 */
  showAuthHint?: boolean;
  children?: ReactNode;
};

function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message || error.title || "요청 실패";
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "알 수 없는 오류";
}

export function ErrorBox({ error, onRetry, showAuthHint, children }: ErrorProps) {
  const apiErr = error instanceof ApiError ? error : null;
  const isAuth = apiErr?.isUnauthorized ?? false;
  return (
    <div
      className="rounded-[var(--radius-card)] border border-[color-mix(in_oklch,var(--color-confidence-low)_30%,var(--color-line))] bg-[color-mix(in_oklch,var(--color-confidence-low)_8%,var(--color-surface-raised))] px-4 py-3 text-sm"
      role="alert"
    >
      <p className="font-medium text-[var(--color-confidence-low)]">
        {isAuth && showAuthHint ? "로그인이 필요합니다" : "요청 실패"}
      </p>
      <p className="mt-1 text-[var(--color-ink-muted)]">{messageOf(error)}</p>
      {onRetry ? (
        <button
          type="button"
          className="mt-2 rounded-[var(--radius-input)] border border-[var(--color-line-strong)] px-2.5 py-1 text-xs font-medium text-[var(--color-ink)] transition hover:bg-[var(--color-surface-sunken)]"
          onClick={onRetry}
        >
          다시 시도
        </button>
      ) : null}
      {children}
    </div>
  );
}

type EmptyProps = {
  label: string;
  hint?: string;
  /** 행동 유도 — 텍스트 + 클릭 핸들러 (Empty CTA) */
  action?: { label: string; onClick: () => void };
};

export function Empty({ label, hint, action }: EmptyProps) {
  return (
    <div className="rounded-[var(--radius-card)] border border-dashed border-[var(--color-line-strong)] bg-[var(--color-surface-sunken)] px-6 py-8 text-center">
      <p className="text-sm font-medium text-[var(--color-ink)]">{label}</p>
      {hint ? <p className="mt-1 text-xs text-[var(--color-ink-muted)]">{hint}</p> : null}
      {action ? (
        <button
          type="button"
          onClick={action.onClick}
          className="mt-3 rounded-[var(--radius-input)] bg-[var(--color-accent)] px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90"
        >
          {action.label}
        </button>
      ) : null}
    </div>
  );
}
